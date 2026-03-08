package com.baratali.difftool.ui;

import com.baratali.difftool.diff.DiffType;
import java.awt.Color;

public final class DiffColors {
    public static final Color ADDED = new Color(183, 232, 196);
    public static final Color REMOVED = new Color(245, 198, 198);
    public static final Color CHANGED = new Color(255, 233, 171);
    public static final Color CURRENT_LINE = new Color(228, 228, 228);
    public static final Color VIEWPORT_LEFT = new Color(72, 111, 184, 100);
    public static final Color VIEWPORT_RIGHT = new Color(19, 123, 84, 100);
    public static final Color RULER_BG = new Color(246, 246, 246);
    public static final Color RULER_BORDER = new Color(210, 210, 210);
    public static final Color GUTTER_BG = new Color(250, 250, 250);
    public static final Color GUTTER_TEXT = new Color(128, 128, 128);

    private DiffColors() {
    }

    public static Color forType(DiffType type) {
        return switch (type) {
            case ADDED -> ADDED;
            case REMOVED -> REMOVED;
            case CHANGED -> CHANGED;
            default -> Color.WHITE;
        };
    }
}
