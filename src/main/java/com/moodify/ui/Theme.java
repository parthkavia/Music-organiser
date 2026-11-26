package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Colours, fonts and global Swing defaults for the dark Moodify look. */
public final class Theme {
    private Theme() {}

    public static final Color BG = new Color(0x0E0E10);
    public static final Color PANEL = new Color(0x161618);
    public static final Color ELEVATED = new Color(0x232326);
    public static final Color HOVER = new Color(0x2C2C31);
    public static final Color BORDER = new Color(0x2E2E33);
    public static final Color ACCENT = new Color(0x8B5CF6);
    public static final Color ACCENT_HOVER = new Color(0xA78BFA);
    public static final Color ACCENT2 = new Color(0xEC4899);
    public static final Color TEXT = new Color(0xF4F4F5);
    public static final Color TEXT_DIM = new Color(0xA1A1AA);
    public static final Color TEXT_FAINT = new Color(0x71717A);
    public static final Color DANGER = new Color(0xF87171);
    public static final Color SUCCESS = new Color(0x34D399);

    private static final String FAMILY = pickFamily();

    private static String pickFamily() {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "SF Pro Text", "Inter", "Helvetica Neue", "Ubuntu", "Noto Sans", "DejaVu Sans"}) {
            if (available.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size)).deriveFont(style, size);
    }

    public static Font regular(float size) { return font(Font.PLAIN, size); }

    public static Font bold(float size) { return font(Font.BOLD, size); }

    public static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Installs the cross-platform look and feel and dark defaults for menus, tooltips etc. */
    public static void install() {
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to whatever is active
        }
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        FontUIResource base = new FontUIResource(regular(13));
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "ComboBox.font", "Menu.font",
                "MenuItem.font", "PopupMenu.font", "ToolTip.font", "List.font", "Table.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "CheckBoxMenuItem.font", "TextArea.font"}) {
            UIManager.put(key, base);
        }
        ColorUIResource bg = new ColorUIResource(ELEVATED);
        ColorUIResource fg = new ColorUIResource(TEXT);
        UIManager.put("PopupMenu.background", bg);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("MenuItem.background", bg);
        UIManager.put("MenuItem.foreground", fg);
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(ACCENT));
        UIManager.put("MenuItem.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("MenuItem.disabledForeground", new ColorUIResource(TEXT_FAINT));
        UIManager.put("MenuItem.acceleratorForeground", new ColorUIResource(TEXT_DIM));
        UIManager.put("MenuItem.acceleratorSelectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("Menu.background", bg);
        UIManager.put("Menu.foreground", fg);
        UIManager.put("Menu.selectionBackground", new ColorUIResource(ACCENT));
        UIManager.put("Menu.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("Menu.disabledForeground", new ColorUIResource(TEXT_FAINT));
        UIManager.put("Separator.foreground", new ColorUIResource(BORDER));
        UIManager.put("Separator.background", bg);
        UIManager.put("ToolTip.background", new ColorUIResource(ELEVATED));
        UIManager.put("ToolTip.foreground", fg);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("Panel.background", new ColorUIResource(BG));
        UIManager.put("OptionPane.background", new ColorUIResource(PANEL));
        UIManager.put("OptionPane.messageForeground", fg);
        UIManager.put("List.background", new ColorUIResource(ELEVATED));
        UIManager.put("List.foreground", fg);
        UIManager.put("List.selectionBackground", new ColorUIResource(ACCENT));
        UIManager.put("List.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("ScrollPane.background", new ColorUIResource(BG));
    }
}
