package com.baratali.difftool.ui;

import com.baratali.difftool.diff.DiffBlock;
import com.baratali.difftool.diff.DiffResult;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.DoubleConsumer;
import javax.swing.JComponent;

public final class OverviewRuler extends JComponent {
    private static final int PREF_WIDTH = 22;

    private DiffResult diffResult;
    private double leftViewportStart;
    private double leftViewportEnd;
    private double rightViewportStart;
    private double rightViewportEnd;
    private DoubleConsumer navigationListener;

    public OverviewRuler() {
        setPreferredSize(new Dimension(PREF_WIDTH, 100));
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                navigate(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                navigate(e);
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setNavigationListener(DoubleConsumer navigationListener) {
        this.navigationListener = navigationListener;
    }

    public void setDiffResult(DiffResult diffResult) {
        this.diffResult = diffResult;
        repaint();
    }

    public void setViewportFractions(double leftStart, double leftEnd, double rightStart, double rightEnd) {
        this.leftViewportStart = leftStart;
        this.leftViewportEnd = leftEnd;
        this.rightViewportStart = rightStart;
        this.rightViewportEnd = rightEnd;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(DiffColors.RULER_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (diffResult != null) {
                List<DiffBlock> blocks = diffResult.blocks();
                int x = 4;
                int width = Math.max(1, getWidth() - 8);
                for (DiffBlock block : blocks) {
                    g2.setColor(DiffColors.forType(block.type()));
                    int y = (int) Math.round(block.overviewStart() * getHeight());
                    int endY = (int) Math.round(block.overviewEnd() * getHeight());
                    int height = Math.max(3, endY - y);
                    g2.fillRoundRect(x, Math.min(y, getHeight() - 1), width, Math.min(height, getHeight() - y), 4, 4);
                }
            }

            paintViewport(g2, leftViewportStart, leftViewportEnd, DiffColors.VIEWPORT_LEFT, 1);
            paintViewport(g2, rightViewportStart, rightViewportEnd, DiffColors.VIEWPORT_RIGHT, getWidth() - 4);

            g2.setColor(DiffColors.RULER_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        } finally {
            g2.dispose();
        }
    }

    private void paintViewport(Graphics2D g2, double start, double end, Color color, int x) {
        if (end <= start) {
            return;
        }
        int top = (int) Math.round(start * getHeight());
        int bottom = (int) Math.round(end * getHeight());
        int height = Math.max(6, bottom - top);
        g2.setColor(color);
        g2.fillRect(x, top, 3, Math.min(height, getHeight() - top));
    }

    private void navigate(MouseEvent event) {
        if (navigationListener == null || getHeight() <= 0) {
            return;
        }
        double fraction = Math.max(0.0, Math.min(1.0, event.getY() / (double) getHeight()));
        navigationListener.accept(fraction);
    }
}
