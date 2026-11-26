package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Left navigation: logo, pages, Import button and the playlist list. */
public final class Sidebar extends JPanel implements Refreshable {
    private final AppContext ctx;
    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private final JPanel playlistBox = new JPanel();

    public Sidebar(AppContext ctx, java.util.function.Consumer<String> onNavigate) {
        super(new BorderLayout());
        this.ctx = ctx;
        setPreferredSize(new Dimension(248, 100));
        setBackground(Theme.PANEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(22, 16, 8, 16));

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel logo = new JLabel(new ImageIcon(Glyph.logo(36)));
        brand.add(logo, BorderLayout.WEST);
        brand.add(UiKit.label("Moodify", Theme.bold(22), Theme.TEXT), BorderLayout.CENTER);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        top.add(brand);
        top.add(Box.createRigidArea(new Dimension(0, 22)));

        addNav(top, "home", Glyph.HOME, "Home", onNavigate);
        addNav(top, "library", Glyph.LIBRARY, "Music Library", onNavigate);
        addNav(top, "playlists", Glyph.PLAYLIST, "Playlists", onNavigate);
        addNav(top, "insights", Glyph.INSIGHTS, "Insights", onNavigate);
        top.add(Box.createRigidArea(new Dimension(0, 16)));

        PillButton imp = new PillButton("Import Music", Glyph.PLUS, PillButton.Style.PRIMARY);
        imp.setAlignmentX(Component.LEFT_ALIGNMENT);
        imp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        imp.setPreferredSize(new Dimension(200, 42));
        imp.addActionListener(e -> ctx.importMusic(this));
        top.add(imp);
        top.add(Box.createRigidArea(new Dimension(0, 22)));

        JPanel plHeader = new JPanel(new BorderLayout());
        plHeader.setOpaque(false);
        plHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        plHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        plHeader.add(UiKit.label("YOUR PLAYLISTS", Theme.bold(11), Theme.TEXT_FAINT), BorderLayout.WEST);
        plHeader.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        IconButton add = new IconButton(Glyph.PLUS, 16, 28, "New playlist").colors(Theme.TEXT_DIM, Color.WHITE);
        add.addActionListener(e -> {
            String n = ctx.createPlaylistPrompt(this);
            if (n != null) ctx.nav().showPlaylists(n);
        });
        plHeader.add(add, BorderLayout.EAST);
        top.add(plHeader);
        add(top, BorderLayout.NORTH);

        playlistBox.setOpaque(false);
        playlistBox.setLayout(new BoxLayout(playlistBox, BoxLayout.Y_AXIS));
        playlistBox.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 12));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(playlistBox, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBackground(Theme.PANEL);
        UiKit.styleScroll(scroll);
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    private void addNav(JPanel parent, String key, Glyph glyph, String label, java.util.function.Consumer<String> nav) {
        NavItem item = new NavItem(glyph, label, 44, 22, () -> nav.accept(key));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        items.put(key, item);
        parent.add(item);
        parent.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    public void setSelected(String key) {
        for (Map.Entry<String, NavItem> e : items.entrySet()) e.getValue().setSelected(e.getKey().equals(key));
    }

    @Override
    public void refresh() {
        playlistBox.removeAll();
        if (ctx.playlists.names().isEmpty()) {
            JLabel none = UiKit.label("No playlists yet", Theme.regular(12), Theme.TEXT_FAINT);
            none.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            playlistBox.add(none);
        }
        for (String name : ctx.playlists.names()) {
            NavItem item = new NavItem(Glyph.NOTE, name, 38, 18, () -> ctx.nav().showPlaylists(name));
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            playlistBox.add(item);
        }
        playlistBox.revalidate();
        playlistBox.repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }
}
