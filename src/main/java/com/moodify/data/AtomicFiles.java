package com.moodify.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Writes a text file via a temp file so a crash never leaves a half-written data file. */
final class AtomicFiles {
    private AtomicFiles() {}

    static void writeLines(Path target, List<String> lines) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = dir.resolve(target.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Keeps a copy of a data file that could not be fully read, so nothing is silently lost. */
    static void backupCorrupt(Path file) {
        try {
            Path bak = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis() + ".bak");
            Files.copy(file, bak, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ignored) {
            // best effort only
        }
    }
}
