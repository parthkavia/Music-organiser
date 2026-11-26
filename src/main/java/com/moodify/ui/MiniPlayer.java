package com.moodify.ui;

import com.moodify.audio.PlayerListener;
import com.moodify.model.RepeatMode;
import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/** The persistent player bar at the bottom of the window. */
public final class MiniPlayer extends JPanel implements PlayerListener, Refreshable {
    private final AppContext ctx;
    private final JLabel cover = new JLabel(CoverArt.icon(60, 8));
    private final JLabel title = UiKit.label("Nothing playing", Theme.bold(14), Theme.TEXT);
    private final JLabel artist = UiKit.label("Pick a song to start", Theme.regular(12), Theme.TEXT_DIM);
    private final IconButton prev = new IconButton(Glyph.PREV, 22, 38, "Previous");
    private final IconButton play = new IconButton(Glyph.PLAY, 20, 42, "Play");
    private final IconButton next = new IconButton(Glyph.NEXT, 22, 38, "Next");
    private final IconButton repeat = new IconButton(Glyph.REPEAT, 20, 34, RepeatMode.OFF.label());
    private final IconButton volumeIcon = new IconButton(Glyph.VOLUME, 20, 34, "Mute");
    private final ModernSlider progress = new ModernSlider();
    private final ModernSlider volume = new ModernSlider();
    private final JLabel elapsed = UiKit.label("0:00", Theme.regular(11), Theme.TEXT_DIM);
    private final JLabel total = UiKit.label("--:--", Theme.regular(11), Theme.TEXT_DIM);
    private long totalMs;
    private double volumeBeforeMute = 0.8;

    public MiniPlayer(AppContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.PANEL);
        setPreferredSize(new Dimension(100, 96));
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0; c.weightx = 0.28; add(buildLeft(), c);
        c.gridx = 1; c.weightx = 0.44; c.insets = new Insets(0, 12, 0, 12); add(buildCenter(), c);
        c.gridx = 2; c.weightx = 0.28; c.insets = new Insets(0, 0, 0, 0); add(buildRight(), c);

        play.circle(Color.WHITE, Theme.ACCENT_HOVER, Color.BLACK);
        play.addActionListener(e -> {
            if (ctx.currentSong() == null) ctx.playAllLibrary();
            else ctx.player.togglePlayPause();
        });
        prev.addActionListener(e -> ctx.player.previous());
        next.addActionListener(e -> ctx.player.next());
        repeat.addActionListener(e -> {
            RepeatMode m = ctx.player.getRepeatMode().next();
            ctx.player.setRepeatMode(m);
            updateRepeat();
        });
        progress.setListener((v, adjusting) -> {
            if (totalMs > 0) elapsed.setText(UiKit.formatClock((long) (v * totalMs)));
            if (!adjusting) ctx.player.seekFraction(v);
        });
        progress.setEnabled(false);
        volume.setValue(ctx.player.getVolume());
        volume.setListener((v, adjusting) -> {
            ctx.player.setVolume(v);
            updateVolumeIcon(v);
        });
        volumeIcon.addActionListener(e -> {
            double v = ctx.player.getVolume();
            if (v > 0.001) {
                volumeBeforeMute = v;
                setVolume(0);
            } else {
                setVolume(volumeBeforeMute <= 0.001 ? 0.6 : volumeBeforeMute);
            }
        });
        ctx.addPlayerListener(this);
        updateRepeat();
    }

    private void setVolume(double v) {
        ctx.player.setVolume(v);
        volume.setValue(v);
        updateVolumeIcon(v);
    }

    private void updateVolumeIcon(double v) {
        volumeIcon.setGlyph(v <= 0.001 ? Glyph.MUTE : Glyph.VOLUME);
        volumeIcon.setToolTipText(v <= 0.001 ? "Unmute" : "Mute");
    }

    private void updateRepeat() {
        RepeatMode m = ctx.player.getRepeatMode();
        repeat.setGlyph(m == RepeatMode.ONE ? Glyph.REPEAT_ONE : Glyph.REPEAT);
        repeat.setActive(m != RepeatMode.OFF);
        repeat.setToolTipText(m.label() + " (click to change)");
    }

    private JPanel buildLeft() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        cover.setPreferredSize(new Dimension(60, 60));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        artist.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(Box.createVerticalGlue());
        text.add(title);
        text.add(Box.createRigidArea(new Dimension(0, 2)));
        text.add(artist);
        text.add(Box.createVerticalGlue());
        text.setMinimumSize(new Dimension(40, 10));
        p.add(cover, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        p.setMinimumSize(new Dimension(120, 10));
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        buttons.add(prev);
        buttons.add(play);
        buttons.add(next);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        elapsed.setPreferredSize(new Dimension(44, 16));
        elapsed.setHorizontalAlignment(JLabel.RIGHT);
        total.setPreferredSize(new Dimension(44, 16));
        bar.add(elapsed, BorderLayout.WEST);
        bar.add(progress, BorderLayout.CENTER);
        bar.add(total, BorderLayout.EAST);
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        p.add(Box.createVerticalGlue());
        p.add(buttons);
        p.add(Box.createRigidArea(new Dimension(0, 2)));
        p.add(bar);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel buildRight() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        p.setOpaque(false);
        volume.setPreferredSize(new Dimension(110, 18));
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.add(repeat);
        wrap.add(Box.createRigidArea(new Dimension(8, 0)));
        wrap.add(volumeIcon);
        wrap.add(volume);
        p.add(wrap);
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.add(p);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(outer, BorderLayout.EAST);
        return right;
    }

    // ------------------------------------------------------------------ PlayerListener (EDT)

    @Override
    public void trackChanged(Song song, int index, int size) {
        title.setText(song.getTitle());
        artist.setText(song.getArtist());
        title.setToolTipText(song.getTitle());
        totalMs = song.getDurationMs();
        total.setText(UiKit.formatTime(totalMs));
        elapsed.setText("0:00");
        progress.setValue(0);
        progress.setEnabled(true);
    }

    @Override
    public void playbackStateChanged(boolean isPlaying) {
        play.setGlyph(isPlaying ? Glyph.PAUSE : Glyph.PLAY);
        play.setToolTipText(isPlaying ? "Pause" : "Play");
    }

    @Override
    public void progress(long currentMs, long totalMsNow) {
        if (totalMsNow > 0 && totalMsNow != totalMs) {
            totalMs = totalMsNow;
            total.setText(UiKit.formatTime(totalMs));
        }
        if (progress.isDragging()) return;
        elapsed.setText(UiKit.formatClock(currentMs));
        progress.setValue(totalMs > 0 ? Math.min(1.0, (double) currentMs / totalMs) : 0);
        progress.setEnabled(totalMs > 0 || ctx.currentSong() != null);
    }

    @Override
    public void queueEnded() {
        play.setGlyph(Glyph.PLAY);
        elapsed.setText("0:00");
        progress.setValue(0);
    }

    @Override
    public void refresh() {
        Song s = ctx.currentSong();
        if (s != null && ctx.library.get(s.getId()) != null) {
            title.setText(s.getTitle());
            artist.setText(s.getArtist());
            if (s.getDurationMs() > 0 && totalMs <= 0) {
                totalMs = s.getDurationMs();
                total.setText(UiKit.formatTime(totalMs));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }
}
