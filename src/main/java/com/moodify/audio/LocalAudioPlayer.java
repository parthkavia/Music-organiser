package com.moodify.audio;

import com.moodify.model.RepeatMode;
import com.moodify.model.Song;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MP3 playback with JavaFX {@link MediaPlayer}. The rest of the application is Swing.
 *
 * <p>Threading rules:
 * <ul>
 *   <li>All public control methods may be called from the Swing EDT; the work is forwarded to the
 *       JavaFX Application Thread with {@link Platform#runLater(Runnable)}.</li>
 *   <li>All player state is only touched on the JavaFX thread.</li>
 *   <li>Every {@link PlayerListener} callback is delivered with {@link SwingUtilities#invokeLater}.</li>
 * </ul></p>
 */
public final class LocalAudioPlayer {
    /** How often progress events are sent to the UI (avoids flooding the EDT). */
    private static final long PROGRESS_INTERVAL_MS = 200;
    private static final double RESTART_THRESHOLD_MS = 3000;

    private static boolean fxReady;
    private static String fxError;

    private final List<PlayerListener> listeners = new CopyOnWriteArrayList<>();

    // --- state below is only touched on the JavaFX thread ---
    private List<Song> queue = new ArrayList<>();
    private int index = -1;
    private MediaPlayer player;
    private final Set<String> failed = new HashSet<>();
    private long lastProgressEmit;
    private boolean errorReported;

    private volatile RepeatMode repeat = RepeatMode.OFF;
    private volatile double volume = 0.8;

    // ================================================================== toolkit

    /** Starts the JavaFX runtime once (without opening any JavaFX window). */
    public static synchronized boolean initToolkit() {
        if (fxReady) return true;
        try {
            try {
                Platform.startup(() -> { });
            } catch (IllegalStateException alreadyRunning) {
                // the toolkit is already running: fine
            }
            Platform.setImplicitExit(false);
            fxReady = true;
            return true;
        } catch (Throwable t) {
            fxError = t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
            return false;
        }
    }

    public static boolean isAvailable() { return fxReady || initToolkit(); }

    /** Stops the JavaFX runtime (call once when the application exits). */
    public static synchronized void shutdown() {
        if (fxReady) {
            try {
                Platform.exit();
            } catch (RuntimeException ignored) {
                // already stopping
            }
        }
    }

    public static String unavailableReason() { return fxError; }

    private void runFx(Runnable r) {
        if (!isAvailable()) {
            fireError("The audio engine could not start" + (fxError == null ? "." : " (" + fxError + ")."));
            return;
        }
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    // ================================================================== listeners

    public void addListener(PlayerListener l) { listeners.add(l); }

    private void fireTrack(Song s, int idx, int size) {
        SwingUtilities.invokeLater(() -> { for (PlayerListener l : listeners) l.trackChanged(s, idx, size); });
    }

    private void firePlaying(boolean playing) {
        SwingUtilities.invokeLater(() -> { for (PlayerListener l : listeners) l.playbackStateChanged(playing); });
    }

    private void fireProgress(long cur, long total) {
        SwingUtilities.invokeLater(() -> { for (PlayerListener l : listeners) l.progress(cur, total); });
    }

    private void fireEnded() {
        SwingUtilities.invokeLater(() -> { for (PlayerListener l : listeners) l.queueEnded(); });
    }

    private void fireError(String msg) {
        SwingUtilities.invokeLater(() -> { for (PlayerListener l : listeners) l.error(msg); });
    }

    // ================================================================== settings

    public RepeatMode getRepeatMode() { return repeat; }

    public void setRepeatMode(RepeatMode mode) { this.repeat = mode == null ? RepeatMode.OFF : mode; }

    public double getVolume() { return volume; }

    public void setVolume(double v) {
        volume = Math.max(0, Math.min(1, v));
        if (!isAvailable()) return;
        runFx(() -> { if (player != null) player.setVolume(volume); });
    }

    // ================================================================== public controls

    /** Replaces the queue with {@code songs} and starts playing the song at {@code startIndex}. */
    public void playQueue(List<Song> songs, int startIndex) {
        List<Song> copy = new ArrayList<>(songs);
        runFx(() -> {
            if (copy.isEmpty() || startIndex < 0 || startIndex >= copy.size()) return;
            Song start = copy.get(startIndex);
            if (!playable(start, false)) {
                fireError("\u201C" + start.getTitle() + "\u201D can't be played because its file is missing. Use Relink MP3 to fix it.");
                return;
            }
            queue = copy;
            failed.clear();
            playAt(startIndex);
        });
    }

    public void togglePlayPause() {
        runFx(() -> {
            if (player == null) {
                int i = nextPlayableIndex(Math.max(index, 0), 1, false);
                if (i >= 0) playAt(i);
                return;
            }
            if (player.getStatus() == MediaPlayer.Status.PLAYING) player.pause();
            else player.play();
        });
    }

    public void pause() {
        runFx(() -> { if (player != null) player.pause(); });
    }

    public void next() {
        runFx(() -> {
            if (queue.isEmpty()) return;
            int i = nextPlayableIndex(index + 1, 1, repeat != RepeatMode.OFF);
            if (i < 0) stopAtEnd();
            else playAt(i);
        });
    }

    public void previous() {
        runFx(() -> {
            if (queue.isEmpty()) return;
            if (player != null && player.getCurrentTime().toMillis() > RESTART_THRESHOLD_MS) {
                player.seek(Duration.ZERO);
                return;
            }
            int i = nextPlayableIndex(index - 1, -1, repeat != RepeatMode.OFF);
            if (i < 0) {
                if (player != null) player.seek(Duration.ZERO);
            } else {
                playAt(i);
            }
        });
    }

    /** Seeks to a position expressed as 0..1 of the track. */
    public void seekFraction(double fraction) {
        runFx(() -> {
            if (player == null) return;
            Duration total = player.getTotalDuration();
            long totalMs = total == null || total.isUnknown() ? currentSong().map(Song::getDurationMs).orElse(0L) : (long) total.toMillis();
            if (totalMs <= 0) return;
            double f = Math.max(0, Math.min(1, fraction));
            player.seek(Duration.millis(f * totalMs));
        });
    }

    public void stop() {
        runFx(() -> {
            disposePlayer();
            queue = new ArrayList<>();
            index = -1;
            firePlaying(false);
            fireProgress(0, 0);
        });
    }

    /** First index in {@code songs} whose file exists, or -1. Used by "play playlist". */
    public static int firstPlayableIndex(List<Song> songs) {
        for (int i = 0; i < songs.size(); i++) if (songs.get(i).fileExists()) return i;
        return -1;
    }

    // ================================================================== internals (FX thread)

    private java.util.Optional<Song> currentSong() {
        return index >= 0 && index < queue.size() ? java.util.Optional.of(queue.get(index)) : java.util.Optional.empty();
    }

    private boolean playable(Song s, boolean checkFailed) {
        if (checkFailed && failed.contains(s.getId())) return false;
        return s.fileExists();
    }

    /** Finds the next playable index starting at {@code start} in direction {@code step}; -1 if none. */
    private int nextPlayableIndex(int start, int step, boolean wrap) {
        int n = queue.size();
        if (n == 0) return -1;
        int idx = start;
        for (int tries = 0; tries < n; tries++) {
            if (idx < 0 || idx >= n) {
                if (!wrap) return -1;
                idx = ((idx % n) + n) % n;
            }
            if (playable(queue.get(idx), true)) return idx;
            idx += step;
        }
        return -1;
    }

    private void playAt(int i) {
        disposePlayer();
        index = i;
        Song song = queue.get(i);
        errorReported = false;
        try {
            Media media = new Media(new File(song.getFilePath()).toURI().toString());
            MediaPlayer mp = new MediaPlayer(media);
            player = mp;
            mp.setVolume(volume);
            mp.setOnReady(() -> {
                if (player != mp) return;
                long total = (long) mp.getTotalDuration().toMillis();
                fireProgress(0, Math.max(total, 0));
            });
            mp.setOnEndOfMedia(() -> { if (player == mp) onTrackEnded(); });
            mp.setOnError(() -> { if (player == mp) onPlaybackError(song, mp.getError() == null ? null : mp.getError().getMessage()); });
            media.setOnError(() -> { if (player == mp) onPlaybackError(song, media.getError() == null ? null : media.getError().getMessage()); });
            mp.statusProperty().addListener((obs, was, now) -> {
                if (player == mp) firePlaying(now == MediaPlayer.Status.PLAYING);
            });
            mp.currentTimeProperty().addListener((obs, was, now) -> {
                if (player != mp) return;
                long t = System.currentTimeMillis();
                if (t - lastProgressEmit < PROGRESS_INTERVAL_MS) return;
                lastProgressEmit = t;
                Duration total = mp.getTotalDuration();
                long totalMs = total == null || total.isUnknown() ? song.getDurationMs() : (long) total.toMillis();
                fireProgress((long) now.toMillis(), totalMs);
            });
            fireTrack(song, i, queue.size());
            fireProgress(0, song.getDurationMs());
            mp.play();
        } catch (Exception e) { // MediaException, IllegalArgumentException, ...
            onPlaybackError(song, e.getMessage());
        }
    }

    private void onTrackEnded() {
        if (repeat == RepeatMode.ONE) {
            player.seek(Duration.ZERO);
            player.play();
            return;
        }
        int i = nextPlayableIndex(index + 1, 1, repeat == RepeatMode.ALL);
        if (i < 0) stopAtEnd();
        else playAt(i);
    }

    private void stopAtEnd() {
        if (player != null) {
            player.pause();
            player.seek(Duration.ZERO);
        }
        firePlaying(false);
        fireProgress(0, currentSong().map(Song::getDurationMs).orElse(0L));
        fireEnded();
    }

    private void onPlaybackError(Song song, String detail) {
        if (errorReported) return;
        errorReported = true;
        failed.add(song.getId());
        fireError("\u201C" + song.getTitle() + "\u201D couldn't be played (unsupported or damaged audio file). Skipping.");
        int i = nextPlayableIndex(index + 1, 1, repeat == RepeatMode.ALL);
        if (i < 0) {
            disposePlayer();
            firePlaying(false);
            fireProgress(0, 0);
            fireEnded();
        } else {
            playAt(i);
        }
    }

    private void disposePlayer() {
        MediaPlayer old = player;
        player = null;
        if (old == null) return;
        try {
            old.setOnEndOfMedia(null);
            old.setOnError(null);
            old.setOnReady(null);
            old.stop();
            old.dispose();
        } catch (RuntimeException ignored) {
            // disposing is best effort
        }
    }

    // ================================================================== probing (duration)

    /** Result of {@link #probe}. durationMs is -1 if the file could not be read. */
    public interface ProbeCallback {
        void done(long durationMs);
    }

    private static final ArrayDeque<Runnable> PROBES = new ArrayDeque<>();
    private static boolean probing;

    /**
     * Measures the duration of an MP3 without playing it.
     * Files are probed one at a time; the callback runs on the Swing EDT.
     */
    public static void probe(String path, ProbeCallback callback) {
        if (!isAvailable()) {
            SwingUtilities.invokeLater(() -> callback.done(-1));
            return;
        }
        Platform.runLater(() -> {
            PROBES.add(() -> runProbe(path, callback));
            pumpProbes();
        });
    }

    private static void pumpProbes() {
        if (probing || PROBES.isEmpty()) return;
        probing = true;
        PROBES.poll().run();
    }

    private static void runProbe(String path, ProbeCallback callback) {
        AtomicBoolean finished = new AtomicBoolean(false);
        MediaPlayer[] holder = new MediaPlayer[1];
        PauseTransition timeout = new PauseTransition(Duration.seconds(10));
        java.util.function.Consumer<Long> finish = dur -> {
            if (!finished.compareAndSet(false, true)) return;
            timeout.stop();
            if (holder[0] != null) {
                try { holder[0].dispose(); } catch (RuntimeException ignored) { /* best effort */ }
            }
            SwingUtilities.invokeLater(() -> callback.done(dur));
            probing = false;
            pumpProbes();
        };
        try {
            if (!Files.isRegularFile(new File(path).toPath())) {
                finish.accept(-1L);
                return;
            }
            Media media = new Media(new File(path).toURI().toString());
            MediaPlayer mp = new MediaPlayer(media);
            holder[0] = mp;
            mp.setOnReady(() -> {
                Duration d = media.getDuration();
                finish.accept(d == null || d.isUnknown() || d.isIndefinite() ? -1L : (long) d.toMillis());
            });
            mp.setOnError(() -> finish.accept(-1L));
            media.setOnError(() -> finish.accept(-1L));
            timeout.setOnFinished(e -> finish.accept(-1L));
            timeout.play();
        } catch (Exception e) {
            finish.accept(-1L);
        }
    }
}
