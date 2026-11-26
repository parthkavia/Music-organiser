package com.moodify;

import com.moodify.audio.LocalAudioPlayer;
import com.moodify.data.DataPaths;
import com.moodify.ui.AppContext;
import com.moodify.ui.MoodifyFrame;
import com.moodify.ui.Theme;

import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Application entry point. */
public final class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() {}

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOG.log(Level.SEVERE, "Unexpected error in " + t.getName(), e));

        // Start the JavaFX audio engine in the background so the window opens immediately.
        Thread fx = new Thread(LocalAudioPlayer::initToolkit, "moodify-fx-init");
        fx.setDaemon(true);
        fx.start();

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            AppContext ctx = new AppContext(DataPaths.dataDir());
            ctx.start();
            MoodifyFrame frame = new MoodifyFrame(ctx);
            frame.setVisible(true);
            ctx.checkMissingFilesAsync();
            ctx.reportStartupProblems();
        });
    }
}
