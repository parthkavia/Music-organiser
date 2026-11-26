package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Function;

/** Themed confirm / prompt dialogs. (Plain information goes to {@link Toast} instead of popups.) */
public final class Dialogs {
    private Dialogs() {}

    private static JLabel wrapped(String text, java.awt.Font font, java.awt.Color color, int width) {
        JLabel l = new JLabel("<html><body style='width:" + width + "px'>" + escape(text).replace("\n", "<br>") + "</body></html>");
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** @return true if the user pressed the confirm button */
    public static boolean confirm(Component parent, String title, String message, String confirmText, boolean danger) {
        boolean[] result = {false};
        BaseDialog d = new BaseDialog(parent, title);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(wrapped(title, Theme.bold(17), Theme.TEXT, 340));
        body.add(javax.swing.Box.createRigidArea(new Dimension(0, 10)));
        body.add(wrapped(message, Theme.regular(13), Theme.TEXT_DIM, 340));
        d.setBody(body);
        d.addButton("Cancel", PillButton.Style.GHOST, d::dispose, false);
        d.addButton(confirmText, danger ? PillButton.Style.DANGER : PillButton.Style.PRIMARY, () -> {
            result[0] = true;
            d.dispose();
        }, true);
        d.open(420);
        return result[0];
    }

    /**
     * Asks for one line of text. The validator returns an error message (keeps the dialog open) or null.
     *
     * @return the text, or null if cancelled
     */
    public static String prompt(Component parent, String title, String message, String initial,
                                String okText, Function<String, String> validator) {
        String[] result = {null};
        BaseDialog d = new BaseDialog(parent, title);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(wrapped(title, Theme.bold(17), Theme.TEXT, 340));
        if (message != null && !message.isBlank()) {
            body.add(javax.swing.Box.createRigidArea(new Dimension(0, 8)));
            body.add(wrapped(message, Theme.regular(13), Theme.TEXT_DIM, 340));
        }
        body.add(javax.swing.Box.createRigidArea(new Dimension(0, 14)));
        StyledField field = new StyledField(null, null);
        field.setText(initial == null ? "" : initial);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        body.add(field);
        JLabel error = new JLabel(" ");
        error.setFont(Theme.regular(12));
        error.setForeground(Theme.DANGER);
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        error.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 0));
        body.add(error);
        d.setBody(body);
        d.addButton("Cancel", PillButton.Style.GHOST, d::dispose, false);
        Runnable ok = () -> {
            String text = field.getText().trim();
            String problem = validator == null ? null : validator.apply(text);
            if (problem != null) {
                error.setText(problem);
                return;
            }
            result[0] = text;
            d.dispose();
        };
        d.addButton(okText, PillButton.Style.PRIMARY, ok, true);
        SwingUtilities.invokeLater(() -> {
            field.requestFocusInWindow();
            field.selectAll();
        });
        d.open(420);
        return result[0];
    }
}
