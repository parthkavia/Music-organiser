package com.moodify;

import com.moodify.data.LibraryQuery;
import com.moodify.data.LibraryStats;
import com.moodify.data.SongLibrary;
import com.moodify.data.SortOption;
import com.moodify.model.Song;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SongLibraryTest {
    @TempDir Path dir;

    private File fake(String name) throws Exception {
        Path p = dir.resolve("music").resolve(name);
        Files.createDirectories(p.getParent());
        Files.write(p, new byte[]{1, 2, 3, 4});
        return p.toFile();
    }

    private SongLibrary lib() { return new SongLibrary(dir.resolve("data")); }

    @Test void importSetsDefaults() throws Exception {
        SongLibrary l = lib();
        var r = l.importFiles(List.of(fake("happy_days_summer.mp3")));
        assertEquals(1, r.added().size());
        Song s = r.added().get(0);
        assertEquals("happy days summer", s.getTitle());
        assertEquals("Unknown artist", s.getArtist());
        assertEquals("Local", s.getGenre());
        assertEquals(Song.SOURCE_LOCAL, s.getSourceType());
        assertEquals(36, s.getId().length());
        assertTrue(Path.of(s.getFilePath()).isAbsolute());
        assertTrue(Files.exists(dir.resolve("data").resolve("songs.txt")));
    }

    @Test void duplicatesAndInvalidFilesAreRejected() throws Exception {
        SongLibrary l = lib();
        File a = fake("a.mp3");
        l.importFiles(List.of(a));
        File txt = dir.resolve("notes.txt").toFile();
        Files.writeString(txt.toPath(), "hi");
        File empty = dir.resolve("empty.mp3").toFile();
        Files.write(empty.toPath(), new byte[0]);
        var r = l.importFiles(List.of(a, txt, empty, new File(dir.toFile(), "ghost.mp3")));
        assertEquals(0, r.added().size());
        assertEquals(1, r.duplicates());
        assertEquals(3, r.rejected().size());
        assertEquals(1, l.size());
    }

    @Test void filesAreNotCopied() throws Exception {
        SongLibrary l = lib();
        File a = fake("keep.mp3");
        l.importFiles(List.of(a));
        assertEquals(a.toPath().toAbsolutePath().normalize().toString(), l.getAll().get(0).getFilePath());
        try (var s = Files.walk(dir.resolve("data"))) {
            assertTrue(s.noneMatch(p -> p.toString().endsWith(".mp3")));
        }
    }

    @Test void persistsEverythingIncludingSpecialCharacters() throws Exception {
        SongLibrary l = lib();
        Song s = l.importFiles(List.of(fake("x.mp3"))).added().get(0);
        l.edit(s.getId(), "Pipe | and\nnewline \u00fc\u4e2d\u6587", "Artist, \"Q\"", "R&B");
        l.applyProbe(s.getId(), 123456);
        l.markPlayed(s.getId());

        SongLibrary again = lib();
        again.load();
        Song t = again.get(s.getId());
        assertNotNull(t);
        assertEquals("Pipe | and\nnewline \u00fc\u4e2d\u6587", t.getTitle());
        assertEquals("Artist, \"Q\"", t.getArtist());
        assertEquals("R&B", t.getGenre());
        assertEquals(123456, t.getDurationMs());
        assertTrue(t.getLastPlayedAt() > 0);
        assertEquals(s.getFilePath(), t.getFilePath());
        assertEquals(0, again.getCorruptedLines());
    }

    @Test void missingFilesAreDetectedAndCanBeRelinked() throws Exception {
        SongLibrary l = lib();
        File f = fake("gone.mp3");
        Song s = l.importFiles(List.of(f)).added().get(0);
        assertFalse(s.isMissing());
        assertTrue(f.delete());
        assertTrue(l.refreshAvailability());
        assertTrue(s.isMissing());
        // entry is retained after restart, still flagged missing
        SongLibrary again = lib();
        again.load();
        assertTrue(again.get(s.getId()).isMissing());

        File replacement = fake("found.mp3");
        assertNull(again.relink(s.getId(), replacement));
        assertFalse(again.get(s.getId()).isMissing());
        assertNotNull(again.relink(s.getId(), new File(dir.toFile(), "nope.mp3")));
    }

    @Test void relinkRefusesFileUsedByAnotherSong() throws Exception {
        SongLibrary l = lib();
        File a = fake("a.mp3"), b = fake("b.mp3");
        l.importFiles(List.of(a, b));
        Song first = l.getAll().get(0);
        assertNotNull(l.relink(first.getId(), b));
    }

    @Test void corruptedLinesAreSkippedAndBackedUp() throws Exception {
        SongLibrary l = lib();
        Song s = l.importFiles(List.of(fake("ok.mp3"))).added().get(0);
        Path file = dir.resolve("data").resolve("songs.txt");
        Files.writeString(file, Files.readString(file) + "garbage line\nid|!!!|a|b|1|LOCAL_FILE|zz\n");
        SongLibrary again = lib();
        again.load();
        assertEquals(1, again.size());
        assertNotNull(again.get(s.getId()));
        assertEquals(2, again.getCorruptedLines());
        try (var st = Files.list(dir.resolve("data"))) {
            assertTrue(st.anyMatch(p -> p.getFileName().toString().contains(".corrupt-")));
        }
    }

    @Test void removeKeepsOthers() throws Exception {
        SongLibrary l = lib();
        l.importFiles(List.of(fake("a.mp3"), fake("b.mp3")));
        String id = l.getAll().get(0).getId();
        assertTrue(l.remove(id));
        assertFalse(l.remove(id));
        assertEquals(1, l.size());
    }

    @Test void legacyFilesWithMoodColumnStillLoad() throws Exception {
        String b = java.util.Base64.getEncoder().encodeToString("Old Song".getBytes());
        String p = java.util.Base64.getEncoder().encodeToString("/music/old.mp3".getBytes());
        String e = java.util.Base64.getEncoder().encodeToString("Local".getBytes());
        Files.createDirectories(dir.resolve("data"));
        Files.writeString(dir.resolve("data").resolve("songs.txt"),
                "abc-123|" + b + "|" + e + "|" + e + "|HAPPY|4200|LOCAL_FILE|" + p + "||true|1000|2000\n");
        SongLibrary l = lib();
        l.load();
        Song s = l.get("abc-123");
        assertNotNull(s);
        assertEquals("Old Song", s.getTitle());
        assertEquals(4200, s.getDurationMs());
        assertEquals("/music/old.mp3", s.getFilePath());
        assertEquals(1000, s.getAddedAt());
        assertEquals(2000, s.getLastPlayedAt());
        assertEquals(0, l.getCorruptedLines());
    }

    @Test void searchFilterSort() {
        Song a = new Song("1", "Beta", "Zed", "Rock", 300, "LOCAL_FILE", "/a");
        Song b = new Song("2", "alpha", "Yan", "Jazz", 100, "LOCAL_FILE", "/b");
        Song c = new Song("3", "Gamma", "Xi", "Folk", 200, "LOCAL_FILE", "/c");
        List<Song> all = List.of(a, b, c);
        assertEquals(List.of(b, a, c), LibraryQuery.apply(all, "", null, SortOption.TITLE_ASC));
        assertEquals(List.of(c, a, b), LibraryQuery.apply(all, null, null, SortOption.TITLE_DESC));
        assertEquals(List.of(a, b, c), LibraryQuery.apply(all, null, null, SortOption.ARTIST_DESC));
        assertEquals(List.of(c, b, a), LibraryQuery.apply(all, null, null, SortOption.ARTIST_ASC));
        assertEquals(List.of(b, c, a), LibraryQuery.apply(all, null, null, SortOption.SHORTEST));
        assertEquals(List.of(a, c, b), LibraryQuery.apply(all, null, null, SortOption.LONGEST));
        assertEquals(List.of(c, b, a), LibraryQuery.apply(all, null, null, SortOption.GENRE_ASC));
        assertEquals(List.of(a), LibraryQuery.apply(all, "zed", null, null));       // artist
        assertEquals(List.of(b), LibraryQuery.apply(all, "JAZZ", null, null));      // genre
        assertEquals(List.of(c), LibraryQuery.apply(all, "gam", null, null));       // title
        assertEquals(List.of(c), LibraryQuery.apply(all, "", List.of("3"), null));
        assertTrue(LibraryQuery.apply(all, "nothing like this", null, null).isEmpty());
    }

    @Test void statsReflectLibrary() {
        Song a = new Song("1", "A", "Zed", "Rock", 60_000, "LOCAL_FILE", "/a");
        Song b = new Song("2", "B", "Zed", "rock", 60_000, "LOCAL_FILE", "/b");
        Song c = new Song("3", "C", "Unknown artist", "Folk", 0, "LOCAL_FILE", "/c");
        c.setMissing(true);
        LibraryStats st = new LibraryStats(List.of(a, b, c));
        assertEquals(3, st.totalSongs());
        assertEquals(120_000, st.totalDurationMs());
        assertEquals(1, st.missingFiles());
        assertEquals(2, (int) st.byGenre().get("Rock"));
        assertEquals(List.of("Zed"), List.copyOf(st.topArtists(5).keySet()));
        assertEquals(1, st.songsWithoutArtist());
    }
}
