package com.moodify.data;

import com.moodify.model.Song;

import java.util.Comparator;

/** Sort choices for the library list. */
public enum SortOption {
    DEFAULT("Date added", null),
    TITLE_ASC("Title A–Z", Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER)),
    TITLE_DESC("Title Z–A", Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER).reversed()),
    ARTIST_ASC("Artist A–Z", Comparator.comparing(Song::getArtist, String.CASE_INSENSITIVE_ORDER)),
    ARTIST_DESC("Artist Z–A", Comparator.comparing(Song::getArtist, String.CASE_INSENSITIVE_ORDER).reversed()),
    SHORTEST("Shortest duration", Comparator.comparingLong(Song::getDurationMs)),
    LONGEST("Longest duration", Comparator.comparingLong(Song::getDurationMs).reversed()),
    GENRE_ASC("Genre A–Z", Comparator.comparing(Song::getGenre, String.CASE_INSENSITIVE_ORDER));

    private final String label;
    private final Comparator<Song> comparator;

    SortOption(String label, Comparator<Song> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    /** null for {@link #DEFAULT}: keep the order the songs were given in. */
    public Comparator<Song> comparator() { return comparator; }

    @Override
    public String toString() { return label; }
}
