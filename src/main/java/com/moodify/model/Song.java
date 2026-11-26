package com.moodify.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** A song in the library. Only the path of the MP3 is stored; the file itself is never copied. */
public final class Song {
    public static final String SOURCE_LOCAL = "LOCAL_FILE";
    public static final String UNKNOWN_ARTIST = "Unknown artist";
    public static final String DEFAULT_GENRE = "Local";

    private final String id;
    private String title;
    private String artist;
    private String genre;
    private long durationMs;
    private String sourceType;
    private String filePath;
    private long addedAt;
    private long lastPlayedAt;
    private volatile boolean missing;

    public Song(String id, String title, String artist, String genre,
                long durationMs, String sourceType, String filePath) {
        this.id = Objects.requireNonNull(id);
        setTitle(title);
        setArtist(artist);
        setGenre(genre);
        this.durationMs = Math.max(0, durationMs);
        this.sourceType = sourceType == null || sourceType.isBlank() ? SOURCE_LOCAL : sourceType;
        this.filePath = filePath;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getGenre() { return genre; }
    public long getDurationMs() { return durationMs; }
    public String getSourceType() { return sourceType; }
    public String getFilePath() { return filePath; }
    public long getAddedAt() { return addedAt; }
    public long getLastPlayedAt() { return lastPlayedAt; }
    public boolean isMissing() { return missing; }

    public void setTitle(String t) { this.title = t == null || t.isBlank() ? "Untitled" : t.trim(); }
    public void setArtist(String a) { this.artist = a == null || a.isBlank() ? UNKNOWN_ARTIST : a.trim(); }
    public void setGenre(String g) { this.genre = g == null || g.isBlank() ? DEFAULT_GENRE : g.trim(); }
    public void setDurationMs(long ms) { this.durationMs = Math.max(0, ms); }
    public void setFilePath(String p) { this.filePath = p; }
    public void setAddedAt(long t) { this.addedAt = t; }
    public void setLastPlayedAt(long t) { this.lastPlayedAt = t; }
    public void setMissing(boolean m) { this.missing = m; }

    public Path path() { return Path.of(filePath); }

    /** Live check against the file system (does not touch the cached {@link #isMissing()} flag). */
    public boolean fileExists() {
        try {
            return filePath != null && Files.isRegularFile(path());
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String toString() { return title + " - " + artist; }
}
