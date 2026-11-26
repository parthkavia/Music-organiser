package com.moodify.model;

/** Playback repeat behaviour. */
public enum RepeatMode {
    OFF("Repeat off"), ALL("Repeat all"), ONE("Repeat one");

    private final String label;

    RepeatMode(String label) { this.label = label; }

    public String label() { return label; }

    public RepeatMode next() { return values()[(ordinal() + 1) % values().length]; }
}
