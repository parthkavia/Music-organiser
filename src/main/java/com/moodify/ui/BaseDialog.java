package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** A modal dialog with the Moodify look: dark body, a content area and a row of buttons. */
public class BaseDialog extends JDialog {
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

    public BaseDialog(Component parent, String title) {
        super(parent == null ? null : SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Theme.PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));
        buttons.setOpaque(false);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().registerKeyboardAction(this::cancelPressed, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    protected void cancelPressed(ActionEvent e) { dispose(); }

    protected void setBody(JComponent c) { getContentPane().add(c, BorderLayout.CENTER); }

    protected PillButton addButton(String text, PillButton.Style style, Runnable action, boolean isDefault) {
        PillButton b = new PillButton(text, style);
        b.addActionListener(e -> action.run());
        buttons.add(b);
        if (isDefault) {
            JRootPane rp = getRootPane();
            rp.setDefaultButton(b);
        }
        return b;
    }

    /** Packs, centres over the parent and shows the dialog (blocks until closed). */
    public void open(int minWidth) {
        pack();
        setSize(Math.max(minWidth, getWidth()), getHeight());
        setResizable(false);
        Window owner = getOwner();
        setLocationRelativeTo(owner);
        setVisible(true);
    }
}
