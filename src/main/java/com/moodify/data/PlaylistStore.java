package com.moodify.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Playlists, persisted to {@code data/playlists.txt}.
 *
 * <p>Each playlist is an ordered set of song IDs ({@link LinkedHashSet}) so a song can never appear
 * twice and the order is preserved. Line format: {@code base64(name)|id1,id2,id3}.</p>
 */
public final class PlaylistStore {
    private static final Logger LOG = Logger.getLogger(PlaylistStore.class.getName());
    public static final int MAX_NAME_LENGTH = 60;

    private final Path file;
    private final Map<String, LinkedHashSet<String>> playlists = new LinkedHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int corruptedLines;

    public PlaylistStore(Path dataDir) {
        this.file = dataDir.resolve("playlists.txt");
    }

    public void addListener(Runnable r) { listeners.add(r); }

    private void changed() {
        save();
        for (Runnable r : listeners) r.run();
    }

    // ------------------------------------------------------------------ queries

    public List<String> names() { return new ArrayList<>(playlists.keySet()); }

    public int count() { return playlists.size(); }

    public boolean exists(String name) { return find(name) != null; }

    /** Song IDs in playlist order (empty list if the playlist does not exist). */
    public List<String> songIds(String name) {
        String key = find(name);
        return key == null ? new ArrayList<>() : new ArrayList<>(playlists.get(key));
    }

    public boolean contains(String name, String songId) {
        String key = find(name);
        return key != null && playlists.get(key).contains(songId);
    }

    public int getCorruptedLines() { return corruptedLines; }

    /** Names of the playlists that contain the song. */
    public List<String> playlistsContaining(String songId) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, LinkedHashSet<String>> e : playlists.entrySet()) {
            if (e.getValue().contains(songId)) out.add(e.getKey());
        }
        return out;
    }

    // ------------------------------------------------------------------ playlist management

    /** @return an error message when the name is not acceptable, otherwise null */
    public String validateName(String name, String ignoreExisting) {
        if (name == null || name.isBlank()) return "Please enter a playlist name.";
        String n = name.trim();
        if (n.length() > MAX_NAME_LENGTH) return "Playlist names can be at most " + MAX_NAME_LENGTH + " characters.";
        for (char c : n.toCharArray()) {
            if (Character.isISOControl(c)) return "Playlist names can't contain control characters.";
        }
        for (String existing : playlists.keySet()) {
            if (existing.equalsIgnoreCase(n) && !existing.equals(ignoreExisting)) {
                return "A playlist called \"" + existing + "\" already exists.";
            }
        }
        return null;
    }

    /** Creates a playlist and returns its (trimmed) name. */
    public String create(String name) {
        String problem = validateName(name, null);
        if (problem != null) throw new IllegalArgumentException(problem);
        String n = name.trim();
        playlists.put(n, new LinkedHashSet<>());
        changed();
        return n;
    }

    public String rename(String oldName, String newName) {
        String key = find(oldName);
        if (key == null) throw new IllegalArgumentException("That playlist no longer exists.");
        String problem = validateName(newName, key);
        if (problem != null) throw new IllegalArgumentException(problem);
        String n = newName.trim();
        if (n.equals(key)) return key;
        Map<String, LinkedHashSet<String>> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> e : playlists.entrySet()) {
            rebuilt.put(e.getKey().equals(key) ? n : e.getKey(), e.getValue());
        }
        playlists.clear();
        playlists.putAll(rebuilt);
        changed();
        return n;
    }

    /** Deletes the playlist only; songs stay in the library. */
    public boolean delete(String name) {
        String key = find(name);
        if (key == null) return false;
        playlists.remove(key);
        changed();
        return true;
    }

    // ------------------------------------------------------------------ songs inside playlists

    /** @return true if the song was added, false if it was already in the playlist */
    public boolean addSong(String name, String songId) {
        String key = find(name);
        if (key == null) throw new IllegalArgumentException("That playlist no longer exists.");
        boolean added = playlists.get(key).add(songId);
        if (added) changed();
        return added;
    }

    /** @return how many of the songs were actually added (duplicates are ignored) */
    public int addSongs(String name, Collection<String> songIds) {
        String key = find(name);
        if (key == null) throw new IllegalArgumentException("That playlist no longer exists.");
        int added = 0;
        for (String id : songIds) if (playlists.get(key).add(id)) added++;
        if (added > 0) changed();
        return added;
    }

    public boolean removeSong(String name, String songId) {
        String key = find(name);
        if (key == null) return false;
        boolean removed = playlists.get(key).remove(songId);
        if (removed) changed();
        return removed;
    }

    /** Moves the song at {@code from} to position {@code to} (both zero-based). */
    public boolean move(String name, int from, int to) {
        String key = find(name);
        if (key == null) return false;
        List<String> ids = new ArrayList<>(playlists.get(key));
        if (from < 0 || from >= ids.size() || to < 0 || to >= ids.size() || from == to) return false;
        String id = ids.remove(from);
        ids.add(to, id);
        playlists.put(key, new LinkedHashSet<>(ids));
        changed();
        return true;
    }

    /** Removes a song from every playlist (used when a song leaves the library). */
    public void removeSongEverywhere(String songId) {
        boolean any = false;
        for (LinkedHashSet<String> set : playlists.values()) any |= set.remove(songId);
        if (any) changed();
    }

    /** Drops IDs that no longer exist in the library (e.g. after a hand-edited data file). */
    public void pruneUnknown(Set<String> validIds) {
        boolean any = false;
        for (LinkedHashSet<String> set : playlists.values()) any |= set.removeIf(id -> !validIds.contains(id));
        if (any) save();
    }

    // ------------------------------------------------------------------ persistence

    public synchronized void load() {
        playlists.clear();
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
                String[] parts = line.split("\\|", -1);
                if (parts.length != 2) throw new IllegalArgumentException("expected 2 fields");
                String name = TextCodec.decode(parts[0]).trim();
                if (name.isEmpty()) throw new IllegalArgumentException("empty name");
                if (find(name) != null) throw new IllegalArgumentException("duplicate playlist");
                LinkedHashSet<String> ids = new LinkedHashSet<>();
                for (String id : parts[1].split(",")) {
                    String t = id.trim();
                    if (!t.isEmpty()) ids.add(t);
                }
                playlists.put(name, ids);
            } catch (RuntimeException e) {
                corruptedLines++;
                LOG.warning("Skipping unreadable playlist line: " + e.getMessage());
            }
        }
        if (corruptedLines > 0) AtomicFiles.backupCorrupt(file);
    }

    public synchronized void save() {
        List<String> lines = new ArrayList<>();
        lines.add("# Moodify playlists v1 | base64(name)|songId,songId,... (order is playlist order)");
        for (Map.Entry<String, LinkedHashSet<String>> e : playlists.entrySet()) {
            lines.add(TextCodec.encode(e.getKey()) + "|" + String.join(",", e.getValue()));
        }
        try {
            AtomicFiles.writeLines(file, lines);
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Could not save " + file, e);
        }
    }

    /** Case-insensitive lookup returning the stored key. */
    private String find(String name) {
        if (name == null) return null;
        for (String k : playlists.keySet()) if (k.equalsIgnoreCase(name.trim())) return k;
        return null;
    }
}
