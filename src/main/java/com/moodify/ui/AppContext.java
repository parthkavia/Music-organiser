package com.moodify.ui;

import com.moodify.audio.LocalAudioPlayer;
import com.moodify.audio.PlayerListener;
import com.moodify.data.PlaylistStore;
import com.moodify.data.SongLibrary;
import com.moodify.model.Song;

import javax.swing.JFileChooser;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wires the data stores, the audio player and the UI together and hosts the user-level actions
 * (import, edit, remove, relink, play ...) that several pages share.
 */
public final class AppContext {
    /** Lets any page ask the main window to switch pages. */
    public interface Navigator {
        void showHome();
        void showLibrary(String playlist);
        void showPlaylists(String name);
        void showInsights();
    }

    public final SongLibrary library;
    public final PlaylistStore playlists;
    public final LocalAudioPlayer player = new LocalAudioPlayer();

    private Navigator navigator;
    private Toast toast;
    private Component dialogParent;
    private Song currentSong;
    private boolean playing;
    private final List<PlayerListener> extraListeners = new CopyOnWriteArrayList<>();
    private static File lastDirectory;

    public AppContext(Path dataDir) {
        this.library = new SongLibrary(dataDir);
        this.playlists = new PlaylistStore(dataDir);
    }

    /** Loads saved data and starts the background file check. Call once, before showing the window. */
    public void start() {
        library.load();
        playlists.load();
        Set<String> ids = new LinkedHashSet<>();
        for (Song s : library.getAll()) ids.add(s.getId());
        playlists.pruneUnknown(ids);

        player.addListener(new PlayerListener() {
            @Override public void trackChanged(Song song, int index, int size) {
                currentSong = song;
                library.markPlayed(song.getId());
            }
            @Override public void playbackStateChanged(boolean p) { playing = p; }
            @Override public void error(String message) { toast(message, true); }
        });
    }

    /** Re-checks (off the UI thread) which MP3 files still exist. */
    public void checkMissingFilesAsync() {
        Thread t = new Thread(() -> {
            Map<String, Boolean> result = library.checkAvailability();
            SwingUtilities.invokeLater(() -> library.applyAvailability(result));
        }, "moodify-file-check");
        t.setDaemon(true);
        t.start();
    }

    public void reportStartupProblems() {
        int bad = library.getCorruptedLines() + playlists.getCorruptedLines();
        if (bad > 0) {
            toast(bad + (bad == 1 ? " damaged entry was" : " damaged entries were")
                    + " skipped while loading. A backup of the original file was kept in the data folder.", true);
        }
    }

    // ------------------------------------------------------------------ wiring

    public void attach(Navigator navigator, Toast toast, Component dialogParent) {
        this.navigator = navigator;
        this.toast = toast;
        this.dialogParent = dialogParent;
    }

    public Navigator nav() { return navigator; }

    public void toast(String message, boolean error) {
        if (toast != null) toast.show(message, error);
    }

    public void toast(String message) { toast(message, false); }

    public Song currentSong() { return currentSong; }

    public boolean isPlaying() { return playing; }

    public void addPlayerListener(PlayerListener l) { player.addListener(l); }

    // ------------------------------------------------------------------ importing

    public void importMusic(Component parent) {
        JFileChooser fc = createChooser();
        fc.setDialogTitle("Import MP3 files");
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileNameExtensionFilter("MP3 audio (*.mp3)", "mp3"));
        if (lastDirectory != null && lastDirectory.isDirectory()) fc.setCurrentDirectory(lastDirectory);
        if (fc.showOpenDialog(parent == null ? dialogParent : parent) != JFileChooser.APPROVE_OPTION) return;
        lastDirectory = fc.getCurrentDirectory();
        File[] selected = fc.getSelectedFiles();
        if (selected == null || selected.length == 0) {
            toast("No files were selected.", true);
            return;
        }
        importFiles(Arrays.asList(selected));
    }

    public void importFiles(List<File> files) {
        SongLibrary.ImportResult r = library.importFiles(files);
        for (Song s : r.added()) {
            LocalAudioPlayer.probe(s.getFilePath(), (durationMs) -> library.applyProbe(s.getId(), durationMs));
        }
        StringBuilder msg = new StringBuilder();
        if (!r.added().isEmpty()) msg.append("Imported ").append(UiKit.plural(r.added().size(), "song", "songs")).append(". ");
        if (r.duplicates() > 0) msg.append(UiKit.plural(r.duplicates(), "song was", "songs were")).append(" already in your library. ");
        if (!r.rejected().isEmpty()) {
            msg.append(r.rejected().size() == 1 ? "Skipped " + r.rejected().get(0) + "." : "Skipped " + r.rejected().size() + " invalid files.");
        }
        if (msg.length() == 0) msg.append("Nothing to import.");
        toast(msg.toString().trim(), r.added().isEmpty());
    }

    private static JFileChooser createChooser() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        boolean nativeLook = os.contains("win") || os.contains("mac");
        LookAndFeel previous = UIManager.getLookAndFeel();
        try {
            if (nativeLook) UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            return new JFileChooser();
        } catch (Exception e) {
            return new JFileChooser();
        } finally {
            if (nativeLook) {
                try { UIManager.setLookAndFeel(previous); } catch (Exception ignored) { /* keep going */ }
            }
        }
    }

    // ------------------------------------------------------------------ song actions

    public void editSong(Component parent, Song song) {
        SongDialog.edit(parent == null ? dialogParent : parent, this, song);
    }

    public void relinkSong(Component parent, Song song) {
        JFileChooser fc = createChooser();
        fc.setDialogTitle("Locate the MP3 for \u201C" + song.getTitle() + "\u201D");
        fc.setMultiSelectionEnabled(false);
        fc.setFileFilter(new FileNameExtensionFilter("MP3 audio (*.mp3)", "mp3"));
        File old = new File(song.getFilePath());
        if (old.getParentFile() != null && old.getParentFile().isDirectory()) fc.setCurrentDirectory(old.getParentFile());
        else if (lastDirectory != null) fc.setCurrentDirectory(lastDirectory);
        if (fc.showOpenDialog(parent == null ? dialogParent : parent) != JFileChooser.APPROVE_OPTION) return;
        File chosen = fc.getSelectedFile();
        if (chosen != null) lastDirectory = chosen.getParentFile();
        String problem = library.relink(song.getId(), chosen);
        if (problem != null) {
            toast(problem, true);
            return;
        }
        LocalAudioPlayer.probe(song.getFilePath(), (durationMs) -> library.applyProbe(song.getId(), durationMs));
        toast("Relinked \u201C" + song.getTitle() + "\u201D.");
    }

    public void removeSongs(Component parent, List<Song> songs) {
        if (songs.isEmpty()) return;
        String what = songs.size() == 1 ? "\u201C" + songs.get(0).getTitle() + "\u201D" : songs.size() + " songs";
        boolean ok = Dialogs.confirm(parent == null ? dialogParent : parent, "Remove from library?",
                "Remove " + what + " from your Moodify library and all playlists?\n"
                        + "The MP3 files on your computer are not deleted.", "Remove", true);
        if (!ok) return;
        for (Song s : new ArrayList<>(songs)) {
            playlists.removeSongEverywhere(s.getId());
            library.remove(s.getId());
        }
        toast("Removed " + what + " from the library.");
    }

    // ------------------------------------------------------------------ playlists

    /** Asks for a name and creates the playlist. @return the new name, or null if cancelled */
    public String createPlaylistPrompt(Component parent) {
        String name = Dialogs.prompt(parent == null ? dialogParent : parent, "New playlist",
                "Give your playlist a name.", "", "Create", t -> playlists.validateName(t, null));
        if (name == null) return null;
        try {
            return playlists.create(name);
        } catch (IllegalArgumentException e) {
            toast(e.getMessage(), true);
            return null;
        }
    }

    public void addToPlaylist(List<Song> songs, String playlistName) {
        List<String> ids = new ArrayList<>();
        for (Song s : songs) ids.add(s.getId());
        try {
            int added = playlists.addSongs(playlistName, ids);
            int skipped = ids.size() - added;
            if (added == 0) toast(skipped == 1 ? "That song is already in \u201C" + playlistName + "\u201D." : "Those songs are already in \u201C" + playlistName + "\u201D.", true);
            else toast("Added " + UiKit.plural(added, "song", "songs") + " to \u201C" + playlistName + "\u201D."
                    + (skipped > 0 ? " " + skipped + " already there." : ""));
        } catch (IllegalArgumentException e) {
            toast(e.getMessage(), true);
        }
    }

    // ------------------------------------------------------------------ playback helpers

    /** Plays {@code queue} starting at {@code index}; reports missing files instead of failing. */
    public void playSongs(List<Song> queue, int index) {
        if (queue.isEmpty() || index < 0 || index >= queue.size()) {
            toast("There's nothing to play yet. Import some MP3 files first.", true);
            return;
        }
        Song s = queue.get(index);
        if (!s.fileExists()) {
            s.setMissing(true);
            library.notifyChanged();
            toast("\u201C" + s.getTitle() + "\u201D can't be found. Right-click it and choose Relink MP3.", true);
            return;
        }
        player.playQueue(queue, index);
    }

    public void playPlaylist(String name) {
        List<Song> songs = library.resolve(playlists.songIds(name));
        if (songs.isEmpty()) {
            toast("\u201C" + name + "\u201D is empty. Add some songs first.", true);
            return;
        }
        int first = LocalAudioPlayer.firstPlayableIndex(songs);
        if (first < 0) {
            toast("None of the songs in \u201C" + name + "\u201D can be found. Relink them to play.", true);
            return;
        }
        player.playQueue(songs, first);
    }

    public void playAllLibrary() {
        List<Song> all = library.getAll();
        if (all.isEmpty()) {
            toast("Your library is empty. Import some MP3 files first.", true);
            return;
        }
        int first = LocalAudioPlayer.firstPlayableIndex(all);
        if (first < 0) {
            toast("None of your songs can be found on disk. Relink them to play.", true);
            return;
        }
        player.playQueue(all, first);
    }
}
