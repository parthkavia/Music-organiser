package com.moodify.data;

import com.moodify.model.Song;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Numbers for the Insights page, always computed from the real library. */
public final class LibraryStats {
    private final int totalSongs;
    private final long totalDurationMs;
    private final int missing;
    private final int unknownArtists;
    private final Map<String, Integer> byGenre;
    private final Map<String, Integer> byArtist;

    public LibraryStats(Collection<Song> songs) {
        long dur = 0;
        int miss = 0;
        int unknown = 0;
        Map<String, Integer> genres = new LinkedHashMap<>();
        Map<String, Integer> artists = new LinkedHashMap<>();
        Map<String, String> genreNames = new LinkedHashMap<>();
        Map<String, String> artistNames = new LinkedHashMap<>();
        for (Song s : songs) {
            dur += s.getDurationMs();
            if (s.isMissing()) miss++;
            String gk = s.getGenre().toLowerCase(Locale.ROOT);
            genreNames.putIfAbsent(gk, s.getGenre());
            genres.merge(gk, 1, Integer::sum);
            if (s.getArtist().equalsIgnoreCase(Song.UNKNOWN_ARTIST)) {
                unknown++;
            } else {
                String ak = s.getArtist().toLowerCase(Locale.ROOT);
                artistNames.putIfAbsent(ak, s.getArtist());
                artists.merge(ak, 1, Integer::sum);
            }
        }
        this.totalSongs = songs.size();
        this.totalDurationMs = dur;
        this.missing = miss;
        this.unknownArtists = unknown;
        this.byGenre = sortByCount(genres, genreNames);
        this.byArtist = sortByCount(artists, artistNames);
    }

    private static Map<String, Integer> sortByCount(Map<String, Integer> counts, Map<String, String> display) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> b.getValue() != a.getValue().intValue()
                ? Integer.compare(b.getValue(), a.getValue()) : a.getKey().compareTo(b.getKey()));
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) out.put(display.get(e.getKey()), e.getValue());
        return out;
    }

    public int totalSongs() { return totalSongs; }
    public long totalDurationMs() { return totalDurationMs; }
    public int missingFiles() { return missing; }
    public int songsWithoutArtist() { return unknownArtists; }
    /** Genre -> count, biggest first. */
    public Map<String, Integer> byGenre() { return byGenre; }
    /** Known artists -> count, biggest first ("Unknown artist" is excluded). */
    public Map<String, Integer> byArtist() { return byArtist; }

    public Map<String, Integer> topArtists(int limit) { return top(byArtist, limit); }
    public Map<String, Integer> topGenres(int limit) { return top(byGenre, limit); }

    private static Map<String, Integer> top(Map<String, Integer> map, int limit) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (out.size() >= limit) break;
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}
