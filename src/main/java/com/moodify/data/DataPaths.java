package com.moodify.data;

import java.nio.file.Path;

/** Where the data files live. Defaults to ./data; override with -Dmoodify.data=/some/dir. */
public final class DataPaths {
    private DataPaths() {}

    public static Path dataDir() {
        String override = System.getProperty("moodify.data");
        return (override == null || override.isBlank() ? Path.of("data") : Path.of(override)).toAbsolutePath();
    }
}
