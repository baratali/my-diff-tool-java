package com.baratali.difftool.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

public final class LineNumberGutter extends JComponent {
    private static final int FIXED_WIDTH = 56;
    private static final int HORIZONTAL_PADDING = 8;

    private final JTextPane textPane;
    private int highlightedLine = -1;

    public LineNumberGutter(JTextPane textPane) {
        this.textPane = textPane;
        setOpaque(true);
        setBackground(DiffColors.GUTTER_BG);
        setForeground(DiffColors.GUTTER_TEXT);
        setFont(textPane.getFont());
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        refresh();
    }

    public void refresh() {
        revalidate();
        repaint();
    }

    public void setHighlightedLine(int highlightedLine) {
        this.highlightedLine = highlightedLine;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(FIXED_WIDTH, textPane.getHeight());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            Rectangle clip = graphics.getClipBounds();
            g2.setColor(getBackground());
            g2.fillRect(clip.x, clip.y, clip.width, clip.height);
            g2.setColor(getForeground());
            g2.setFont(getFont());

            int startOffset = textPane.viewToModel2D(new java.awt.Point(0, clip.y));
            int endOffset = textPane.viewToModel2D(new java.awt.Point(0, clip.y + clip.height));
            Element root = textPane.getDocument().getDefaultRootElement();
            int startLine = root.getElementIndex(Math.max(0, startOffset));
            int endLine = root.getElementIndex(Math.max(0, endOffset));

            FontMetrics metrics = g2.getFontMetrics();
            int availableWidth = getWidth() - HORIZONTAL_PADDING;
            int baselineAdjust = metrics.getAscent();

            for (int line = startLine; line <= endLine; line++) {
                Element element = root.getElement(line);
                try {
                    Rectangle lineRect = SwingUtilities.convertRectangle(
                            textPane,
                            textPane.modelToView2D(element.getStartOffset()).getBounds(),
                            this
                    );
                    if (line == highlightedLine) {
                        g2.setColor(DiffColors.CURRENT_LINE);
                        g2.fillRoundRect(4, lineRect.y + 1, getWidth() - 8, Math.max(4, lineRect.height - 2), 8, 8);
                        g2.setColor(getForeground());
                    }
                    String label = Integer.toString(line + 1);
                    int x = availableWidth - metrics.stringWidth(label);
                    g2.drawString(label, x, lineRect.y + baselineAdjust);
                } catch (BadLocationException ignored) {
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
