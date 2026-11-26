package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.Component;

/** Big page title with a dim subtitle underneath. */
final class PageHeader extends JPanel {
    private final JLabel title;
    private final JLabel subtitle;

    PageHeader(String titleText, String subtitleText) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        title = UiKit.label(titleText, Theme.bold(28), Theme.TEXT);
        subtitle = UiKit.label(subtitleText, Theme.regular(13), Theme.TEXT_DIM);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        add(title);
        add(subtitle);
    }

    void setSubtitle(String text) { subtitle.setText(text); }

    void setTitle(String text) { title.setText(text); }
}
