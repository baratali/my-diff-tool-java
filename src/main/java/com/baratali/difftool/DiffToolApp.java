package com.baratali.difftool;

import com.baratali.difftool.ui.DiffToolFrame;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class DiffToolApp {
    private static final String APP_NAME = "Diff Tool";

    static {
        configureApplicationIdentity();
    }

    private DiffToolApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installSystemLookAndFeel();
            DiffToolFrame frame = new DiffToolFrame(selectEditorFontFamily());
            Image icon = createAppIcon();
            frame.setIconImage(icon);
            installTaskbarIcon(icon);
            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        });
    }

    private static void configureApplicationIdentity() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", APP_NAME);
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);
    }

    private static void installSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private static String selectEditorFontFamily() {
        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        return Arrays.stream(families)
                .filter(name -> "Iosevka".equalsIgnoreCase(name)
                        || name.toLowerCase().startsWith("iosevka "))
                .findFirst()
                .orElse("Monospaced");
    }

    private static void installTaskbarIcon(Image icon) {
        if (!Taskbar.isTaskbarSupported()) {
            return;
        }
        try {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(icon);
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
    }

    private static Image createAppIcon() {
        int size = 128;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(245, 247, 250));
            g2.fillRoundRect(8, 8, 112, 112, 26, 26);

            g2.setColor(new Color(222, 227, 234));
            g2.fillRoundRect(16, 16, 40, 96, 18, 18);
            g2.fillRoundRect(72, 16, 40, 96, 18, 18);
            g2.fillRoundRect(58, 16, 12, 96, 8, 8);

            g2.setColor(new Color(217, 74, 74));
            g2.fillRoundRect(24, 30, 24, 10, 6, 6);
            g2.fillRoundRect(24, 52, 16, 10, 6, 6);
            g2.fillRoundRect(24, 74, 22, 10, 6, 6);

            g2.setColor(new Color(64, 157, 104));
            g2.fillRoundRect(80, 30, 18, 10, 6, 6);
            g2.fillRoundRect(80, 52, 24, 10, 6, 6);
            g2.fillRoundRect(80, 74, 16, 10, 6, 6);

            g2.setColor(new Color(214, 171, 62));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(52, 38, 76, 52);
            g2.drawLine(52, 74, 76, 60);
        } finally {
            g2.dispose();
        }
        return image;
    }
}
