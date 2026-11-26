package com.moodify.data;

import com.moodify.model.Song;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Search + playlist filter + sort for the library view (pure logic, easy to test). */
public final class LibraryQuery {
    private LibraryQuery() {}

    /**
     * @param text        search text (title, artist, genre); every word must match; may be null/blank
     * @param allowedIds  only songs with these IDs (a playlist filter); null = any
     * @param sort        sort order; null or DEFAULT keeps the input order
     */
    public static List<Song> apply(Collection<Song> songs, String text,
                                   Collection<String> allowedIds, SortOption sort) {
        String[] terms = text == null || text.isBlank()
                ? new String[0] : text.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<Song> out = new ArrayList<>();
        for (Song s : songs) {
            if (allowedIds != null && !allowedIds.contains(s.getId())) continue;
            if (terms.length > 0) {
                String hay = (s.getTitle() + " " + s.getArtist() + " " + s.getGenre()).toLowerCase(Locale.ROOT);
                boolean all = true;
                for (String t : terms) {
                    if (!hay.contains(t)) { all = false; break; }
                }
                if (!all) continue;
            }
            out.add(s);
        }
        if (sort != null && sort.comparator() != null) out.sort(sort.comparator());
        return out;
    }
}
