package com.moodify.data;

import com.moodify.model.Song;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The song library: all songs, plus persistence to {@code data/songs.txt}.
 *
 * <p>Line format (fields separated by '|', text fields Base64 encoded):
 * {@code id|title|artist|genre|durationMs|sourceType|path|addedAt|lastPlayedAt}.
 * The last two columns are optional. Files written by older versions (which had a mood column) still load.</p>
 *
 * <p>Not thread-safe by design: call mutating methods from one thread (the Swing EDT).</p>
 */
public final class SongLibrary {
    private static final Logger LOG = Logger.getLogger(SongLibrary.class.getName());
    private static final String HEADER = "# Moodify songs v2 | id|title|artist|genre|durationMs|sourceType|path|addedAt|lastPlayedAt (text fields are Base64)";

    /** Outcome of an import: what was added, how many duplicates were skipped, and what was refused. */
    public record ImportResult(List<Song> added, int duplicates, List<String> rejected) {}

    private final Path file;
    private final Map<String, Song> songs = new LinkedHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int corruptedLines;
    private String lastSaveError;

    public SongLibrary(Path dataDir) {
        this.file = dataDir.resolve("songs.txt");
    }

    // ------------------------------------------------------------------ listeners

    public void addListener(Runnable r) { listeners.add(r); }

    public void notifyChanged() { for (Runnable r : listeners) r.run(); }

    // ------------------------------------------------------------------ queries

    public List<Song> getAll() { return new ArrayList<>(songs.values()); }

    public Song get(String id) { return songs.get(id); }

    public int size() { return songs.size(); }

    public int getCorruptedLines() { return corruptedLines; }

    public String getLastSaveError() { return lastSaveError; }

    public List<Song> resolve(Collection<String> ids) {
        List<Song> out = new ArrayList<>();
        for (String id : ids) {
            Song s = songs.get(id);
            if (s != null) out.add(s);
        }
        return out;
    }

    // ------------------------------------------------------------------ import / edit / remove

    public ImportResult importFiles(List<File> files) {
        List<Song> added = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        int duplicates = 0;
        Map<String, Boolean> known = new HashMap<>();
        for (Song s : songs.values()) known.put(pathKey(s.getFilePath()), Boolean.TRUE);

        for (File f : files) {
            if (f == null) continue;
            String problem = validateMp3(f);
            if (problem != null) {
                rejected.add(f.getName() + " (" + problem + ")");
                continue;
            }
            String path = f.toPath().toAbsolutePath().normalize().toString();
            String key = pathKey(path);
            if (known.containsKey(key)) {
                duplicates++;
                continue;
            }
            known.put(key, Boolean.TRUE);
            Song s = new Song(UUID.randomUUID().toString(), titleFromFileName(f.getName()),
                    Song.UNKNOWN_ARTIST, Song.DEFAULT_GENRE, 0, Song.SOURCE_LOCAL, path);
            s.setAddedAt(System.currentTimeMillis() + added.size()); // keeps import order stable
            songs.put(s.getId(), s);
            added.add(s);
        }
        if (!added.isEmpty()) {
            save();
            notifyChanged();
        }
        return new ImportResult(added, duplicates, rejected);
    }

    /** Returns null when the file looks like an importable MP3, otherwise a short reason. */
    public static String validateMp3(File f) {
        if (!f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3")) return "not an MP3 file";
        if (!f.isFile()) return "file not found";
        if (!f.canRead()) return "file cannot be read";
        if (f.length() == 0) return "file is empty";
        return null;
    }

    public static String titleFromFileName(String fileName) {
        String t = fileName;
        int dot = t.lastIndexOf('.');
        if (dot > 0) t = t.substring(0, dot);
        t = t.replace('_', ' ').replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? "Untitled" : t;
    }

    /** Edits a song's title, artist and genre. */
    public void edit(String id, String title, String artist, String genre) {
        Song s = songs.get(id);
        if (s == null) return;
        s.setTitle(title);
        s.setArtist(artist);
        s.setGenre(genre);
        save();
        notifyChanged();
    }

    /** Stores the duration the audio engine measured for a freshly imported file. */
    public void applyProbe(String id, long durationMs) {
        Song s = songs.get(id);
        if (s == null || durationMs <= 0 || s.getDurationMs() == durationMs) return;
        s.setDurationMs(durationMs);
        save();
        notifyChanged();
    }

    public boolean remove(String id) {
        if (songs.remove(id) == null) return false;
        save();
        notifyChanged();
        return true;
    }

    /**
     * Points a song at a different MP3 file.
     *
     * @return the problem as a message, or null on success
     */
    public String relink(String id, File newFile) {
        Song s = songs.get(id);
        if (s == null) return "That song is no longer in the library.";
        String problem = validateMp3(newFile);
        if (problem != null) return "Can't use that file: " + problem + ".";
        String path = newFile.toPath().toAbsolutePath().normalize().toString();
        String key = pathKey(path);
        for (Song other : songs.values()) {
            if (other != s && pathKey(other.getFilePath()).equals(key)) {
                return "Another song in the library already uses that file.";
            }
        }
        s.setFilePath(path);
        s.setMissing(false);
        s.setDurationMs(0); // re-measured by the caller
        save();
        notifyChanged();
        return null;
    }

    /** Remembers when a song was last played (used for "Recently played"). Does not notify listeners. */
    public void markPlayed(String id) {
        Song s = songs.get(id);
        if (s == null) return;
        s.setLastPlayedAt(System.currentTimeMillis());
        save();
    }

    // ------------------------------------------------------------------ missing-file detection

    /** Slow part (touches the disk). Safe to call from a background thread. */
    public Map<String, Boolean> checkAvailability() {
        List<Song> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(songs.values());
        }
        Map<String, Boolean> result = new HashMap<>();
        for (Song s : snapshot) result.put(s.getId(), !s.fileExists());
        return result;
    }

    /** Applies a {@link #checkAvailability()} result; returns true if any flag changed. */
    public boolean applyAvailability(Map<String, Boolean> missingById) {
        boolean changed = false;
        for (Map.Entry<String, Boolean> e : missingById.entrySet()) {
            Song s = songs.get(e.getKey());
            if (s != null && s.isMissing() != e.getValue()) {
                s.setMissing(e.getValue());
                changed = true;
            }
        }
        if (changed) notifyChanged();
        return changed;
    }

    /** Synchronous convenience (used at startup and in tests). */
    public boolean refreshAvailability() { return applyAvailability(checkAvailability()); }

    // ------------------------------------------------------------------ persistence

    public synchronized void load() {
        songs.clear();
        corruptedLines = 0;
        if (!Files.exists(file)) return;
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Could not read " + file, e);
            corruptedLines = 1;
            AtomicFiles.backupCorrupt(file);
            return;
        }
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            try {
                Song s = parse(line);
                if (songs.containsKey(s.getId())) throw new IllegalArgumentException("duplicate id");
                songs.put(s.getId(), s);
            } catch (RuntimeException e) {
                corruptedLines++;
                LOG.warning("Skipping unreadable song line: " + e.getMessage());
            }
        }
        if (corruptedLines > 0) AtomicFiles.backupCorrupt(file);
        refreshAvailability();
    }

    public synchronized void save() {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Song s : songs.values()) lines.add(serialize(s));
        try {
            AtomicFiles.writeLines(file, lines);
            lastSaveError = null;
        } catch (IOException | RuntimeException e) {
            lastSaveError = e.getMessage();
            LOG.log(Level.WARNING, "Could not save " + file, e);
        }
    }

    static String serialize(Song s) {
        return String.join("|",
                s.getId(),
                TextCodec.encode(s.getTitle()),
                TextCodec.encode(s.getArtist()),
                TextCodec.encode(s.getGenre()),
                Long.toString(s.getDurationMs()),
                s.getSourceType(),
                TextCodec.encode(s.getFilePath()),
                Long.toString(s.getAddedAt()),
                Long.toString(s.getLastPlayedAt()));
    }

    static Song parse(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length < 7) throw new IllegalArgumentException("expected at least 7 fields but found " + f.length);
        // Older files had a mood name in column 4 (id|title|artist|genre|MOOD|duration|source|path|album|auto|added|played).
        if (!f[4].trim().matches("\\d+")) {
            if (f.length < 8) throw new IllegalArgumentException("unrecognised line");
            f = new String[]{f[0], f[1], f[2], f[3], f[5], f[6], f[7],
                    f.length > 10 ? f[10] : "", f.length > 11 ? f[11] : ""};
        }
        String id = f[0].trim();
        if (!id.matches("[A-Za-z0-9\\-]{1,64}")) throw new IllegalArgumentException("bad id");
        String path = TextCodec.decode(f[6]);
        if (path.isBlank()) throw new IllegalArgumentException("empty path");
        Song s = new Song(id, TextCodec.decode(f[1]), TextCodec.decode(f[2]), TextCodec.decode(f[3]),
                Long.parseLong(f[4].trim()), f[5].trim(), path);
        if (f.length > 7 && !f[7].isBlank()) s.setAddedAt(Long.parseLong(f[7].trim()));
        if (f.length > 8 && !f[8].isBlank()) s.setLastPlayedAt(Long.parseLong(f[8].trim()));
        return s;
    }

    private static String pathKey(String path) {
        String p = Path.of(path).toAbsolutePath().normalize().toString();
        return File.separatorChar == '\\' ? p.toLowerCase(Locale.ROOT) : p;
    }
}
