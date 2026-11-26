package com.moodify;

import com.moodify.data.PlaylistStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistStoreTest {
    @TempDir Path dir;

    @Test void createValidatesNames() {
        PlaylistStore p = new PlaylistStore(dir);
        assertEquals("Road trip", p.create("  Road trip "));
        assertThrows(IllegalArgumentException.class, () -> p.create("   "));
        assertThrows(IllegalArgumentException.class, () -> p.create("road TRIP"));
        assertThrows(IllegalArgumentException.class, () -> p.create("x".repeat(PlaylistStore.MAX_NAME_LENGTH + 1)));
        assertEquals(1, p.count());
    }

    @Test void noDuplicateSongsAndOrderPreserved() {
        PlaylistStore p = new PlaylistStore(dir);
        p.create("A");
        assertTrue(p.addSong("A", "s1"));
        assertTrue(p.addSong("A", "s2"));
        assertTrue(p.addSong("A", "s3"));
        assertFalse(p.addSong("A", "s2"));
        assertEquals(List.of("s1", "s2", "s3"), p.songIds("A"));
        assertEquals(1, p.addSongs("A", List.of("s3", "s4")));
    }

    @Test void reorderAndRemove() {
        PlaylistStore p = new PlaylistStore(dir);
        p.create("A");
        p.addSongs("A", List.of("1", "2", "3", "4"));
        assertTrue(p.move("A", 3, 0));
        assertEquals(List.of("4", "1", "2", "3"), p.songIds("A"));
        assertTrue(p.move("A", 0, 2));
        assertEquals(List.of("1", "2", "4", "3"), p.songIds("A"));
        assertFalse(p.move("A", 0, 9));
        assertTrue(p.removeSong("A", "2"));
        assertEquals(List.of("1", "4", "3"), p.songIds("A"));
    }

    @Test void persistsOrderAndSpecialNames() {
        PlaylistStore p = new PlaylistStore(dir);
        String name = p.create("Mix | \u00e9\u00e8 \u2764 \"quotes\"");
        p.addSongs(name, List.of("c", "a", "b"));
        p.create("Empty one");

        PlaylistStore q = new PlaylistStore(dir);
        q.load();
        assertEquals(List.of(name, "Empty one"), q.names());
        assertEquals(List.of("c", "a", "b"), q.songIds(name));
        assertEquals(0, q.getCorruptedLines());
    }

    @Test void deletingPlaylistLeavesOtherDataAndRenameKeepsSongs() {
        PlaylistStore p = new PlaylistStore(dir);
        p.create("A");
        p.create("B");
        p.addSong("A", "x");
        p.rename("A", "Alpha");
        assertEquals(List.of("x"), p.songIds("alpha"));
        assertTrue(p.delete("B"));
        assertEquals(List.of("Alpha"), p.names());
    }

    @Test void removeSongEverywhereAndPrune() {
        PlaylistStore p = new PlaylistStore(dir);
        p.create("A");
        p.create("B");
        p.addSongs("A", List.of("1", "2"));
        p.addSongs("B", List.of("2", "3"));
        p.removeSongEverywhere("2");
        assertEquals(List.of("1"), p.songIds("A"));
        assertEquals(List.of("3"), p.songIds("B"));
        p.pruneUnknown(Set.of("1"));
        assertTrue(p.songIds("B").isEmpty());
    }

    @Test void corruptedFileIsHandledGracefully() throws Exception {
        String good = java.util.Base64.getEncoder().encodeToString("Good".getBytes());
        Files.writeString(dir.resolve("playlists.txt"), "this is not valid\n!!!|a,b\n" + good + "|x,y\n");
        PlaylistStore p = new PlaylistStore(dir);
        p.load();
        assertEquals(List.of("Good"), p.names());
        assertEquals(2, p.getCorruptedLines());
    }
}
