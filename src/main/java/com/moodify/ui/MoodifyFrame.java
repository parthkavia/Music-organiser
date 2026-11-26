package com.moodify.ui;

import com.moodify.audio.LocalAudioPlayer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** The main window: sidebar, page area and the persistent mini-player. */
public final class MoodifyFrame extends JFrame implements AppContext.Navigator {
    private final AppContext ctx;
    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);
    private final HomePage home;
    private final LibraryPage library;
    private final PlaylistsPage playlistsPage;
    private final InsightsPage insights;
    private final Sidebar sidebar;
    private final MiniPlayer mini;
    private final Toast toast;
    private final Timer refreshTimer;
    private String currentPage = "home";

    public MoodifyFrame(AppContext ctx) {
        super("Moodify");
        this.ctx = ctx;
        setIconImage(Glyph.logo(128));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1020, 680));
        setSize(1320, 820);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);

        home = new HomePage(ctx);
        library = new LibraryPage(ctx);
        playlistsPage = new PlaylistsPage(ctx);
        insights = new InsightsPage(ctx);
        mini = new MiniPlayer(ctx);
        sidebar = new Sidebar(ctx, this::show);

        deck.setBackground(Theme.BG);
        deck.add(home, "home");
        deck.add(library, "library");
        deck.add(playlistsPage, "playlists");
        deck.add(insights, "insights");

        root.add(sidebar, BorderLayout.WEST);
        root.add(deck, BorderLayout.CENTER);
        root.add(mini, BorderLayout.SOUTH);

        toast = new Toast(getLayeredPane());
        ctx.attach(this, toast, this);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { toast.reposition(); }
        });

        // library / playlist changes redraw every page (coalesced so bursts of changes cost one redraw)
        refreshTimer = new Timer(60, e -> refreshAll());
        refreshTimer.setRepeats(false);
        ctx.library.addListener(this::scheduleRefresh);
        ctx.playlists.addListener(this::scheduleRefresh);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { quit(); }
            @Override public void windowActivated(WindowEvent e) { ctx.checkMissingFilesAsync(); }
        });
        installSpaceShortcut();
        show("home");
    }

    private void scheduleRefresh() {
        if (SwingUtilities.isEventDispatchThread()) refreshTimer.restart();
        else SwingUtilities.invokeLater(refreshTimer::restart);
    }

    private void refreshAll() {
        home.refresh();
        library.refresh();
        playlistsPage.refresh();
        insights.refresh();
        sidebar.refresh();
        mini.refresh();
    }

    /** Space toggles play/pause unless the user is typing or focus is on a button. */
    private void installSpaceShortcut() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || e.getKeyCode() != KeyEvent.VK_SPACE || e.getModifiersEx() != 0) return false;
            if (!isActive()) return false;
            java.awt.Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (owner instanceof JTextComponent || owner instanceof javax.swing.AbstractButton) return false;
            if (ctx.currentSong() == null) return false;
            ctx.player.togglePlayPause();
            return true;
        });
    }

    // ------------------------------------------------------------------ navigation

    public void show(String page) {
        currentPage = page;
        cards.show(deck, page);
        sidebar.setSelected(page);
        JComponent p = switch (page) {
            case "home" -> home;
            case "library" -> library;
            case "playlists" -> playlistsPage;
            default -> insights;
        };
        ((Refreshable) p).refresh();
    }

    @Override public void showHome() { show("home"); }

    @Override public void showLibrary(String playlist) {
        library.showPlaylist(playlist);
        show("library");
    }

    @Override public void showPlaylists(String name) {
        playlistsPage.select(name);
        show("playlists");
    }

    @Override public void showInsights() { show("insights"); }

    public String currentPage() { return currentPage; }

    // ------------------------------------------------------------------ shutdown

    private void quit() {
        try {
            ctx.player.stop();
            ctx.library.save();
            ctx.playlists.save();
        } finally {
            dispose();
            LocalAudioPlayer.shutdown();
            System.exit(0);
        }
    }
}
