package com.moodify.audio;

import com.moodify.model.Song;

/** Callbacks from {@link LocalAudioPlayer}. They are always delivered on the Swing Event Dispatch Thread. */
public interface PlayerListener {
    /** A new track started loading/playing. */
    default void trackChanged(Song song, int indexInQueue, int queueSize) {}

    /** Playing (true) or paused/stopped (false). */
    default void playbackStateChanged(boolean playing) {}

    /** Throttled position updates. totalMs may be 0 if unknown. */
    default void progress(long currentMs, long totalMs) {}

    /** The queue finished (Repeat OFF) and playback stopped on the last track. */
    default void queueEnded() {}

    /** Something went wrong; the message is user-friendly. Playback continues with the next song if possible. */
    default void error(String message) {}
}
