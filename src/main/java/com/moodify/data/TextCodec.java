package com.moodify.data;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 helpers for the text fields in the data files, so that separators, new lines and any
 * unicode character can be stored safely. Base64 is an ENCODING (not encryption): it protects the
 * file format, not the content.
 */
public final class TextCodec {
    private TextCodec() {}

    public static String encode(String text) {
        String t = text == null ? "" : text;
        return Base64.getEncoder().encodeToString(t.getBytes(StandardCharsets.UTF_8));
    }

    /** @throws IllegalArgumentException when the value is not valid Base64 */
    public static String decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
