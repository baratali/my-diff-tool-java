package com.baratali.difftool.ui;

import com.baratali.difftool.diff.DiffEngine;
import com.baratali.difftool.diff.DiffResult;
import com.baratali.difftool.diff.DiffStats;
import com.baratali.difftool.diff.DiffType;
import com.baratali.difftool.diff.HighlightSpan;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public final class DiffToolFrame extends JFrame {
    private final JTextPane leftPane = new JTextPane(new DefaultStyledDocument());
    private final JTextPane rightPane = new JTextPane(new DefaultStyledDocument());
    private final JScrollPane leftScrollPane = new JScrollPane(leftPane);
    private final JScrollPane rightScrollPane = new JScrollPane(rightPane);
    private final LineNumberGutter leftGutter = new LineNumberGutter(leftPane);
    private final LineNumberGutter rightGutter = new LineNumberGutter(rightPane);
    private final OverviewRuler overviewRuler = new OverviewRuler();
    private final JLabel statusLabel = new JLabel("Paste text into both panes to compare.");
    private final DiffEngine diffEngine = new DiffEngine();
    private final Timer diffDebounceTimer;
    private final Highlighter.HighlightPainter currentLinePainter =
            new DefaultHighlighter.DefaultHighlightPainter(DiffColors.CURRENT_LINE);

    private boolean applyingHighlights;
    private boolean syncingScroll;
    private DiffResult currentResult = diffEngine.compare("", "");
    private Object leftCurrentLineHighlight;
    private Object rightCurrentLineHighlight;
    private JTextPane activePane;

    public DiffToolFrame(String editorFontFamily) {
        super("Diff Tool");
        configureEditors(editorFontFamily);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setPreferredSize(new Dimension(1280, 820));
        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(null);

        diffDebounceTimer = new Timer(180, event -> recomputeDiff());
        diffDebounceTimer.setRepeats(false);

        installDocumentListeners();
        installScrollSync();
        installCaretSync();
        overviewRuler.setNavigationListener(this::scrollToFraction);
        recomputeDiff();
        activePane = leftPane;
        updateCurrentLineHighlights();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel editors = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 8);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.49;
        editors.add(wrapPane("Original", leftScrollPane), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.02;
        gbc.insets = new Insets(0, 0, 0, 8);
        editors.add(overviewRuler, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.49;
        gbc.insets = new Insets(0, 0, 0, 0);
        editors.add(wrapPane("Modified", rightScrollPane), gbc);

        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 4));

        root.add(editors, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private JPanel wrapPane(String title, JScrollPane scrollPane) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setMinimumSize(new Dimension(0, 0));
        panel.setMinimumSize(new Dimension(0, 0));
        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void configureEditors(String editorFontFamily) {
        Font editorFont = new Font(editorFontFamily, Font.PLAIN, 14);
        configureEditor(leftPane, editorFont);
        configureEditor(rightPane, editorFont);
        leftGutter.setFont(editorFont);
        rightGutter.setFont(editorFont);
        leftScrollPane.setRowHeaderView(leftGutter);
        rightScrollPane.setRowHeaderView(rightGutter);
    }

    private void configureEditor(JTextPane pane, Font editorFont) {
        pane.setFont(editorFont);
        pane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        pane.setMargin(new Insets(0, 0, 0, 0));
        pane.setMinimumSize(new Dimension(0, 0));
        pane.setPreferredSize(new Dimension(0, 0));
    }

    private void installDocumentListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                queueDiff();
                refreshLineNumbers();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                queueDiff();
                refreshLineNumbers();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                queueDiff();
                refreshLineNumbers();
            }
        };
        leftPane.getDocument().addDocumentListener(listener);
        rightPane.getDocument().addDocumentListener(listener);
    }

    private void installScrollSync() {
        leftScrollPane.getVerticalScrollBar().addAdjustmentListener(event -> syncScrollFrom(leftPane, rightPane));
        rightScrollPane.getVerticalScrollBar().addAdjustmentListener(event -> syncScrollFrom(rightPane, leftPane));
        leftScrollPane.getViewport().addChangeListener(event -> leftGutter.repaint());
        rightScrollPane.getViewport().addChangeListener(event -> rightGutter.repaint());
    }

    private void installCaretSync() {
        CaretListener listener = new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent event) {
                activePane = event.getSource() == leftPane ? leftPane : rightPane;
                updateCurrentLineHighlights();
            }
        };
        leftPane.addCaretListener(listener);
        rightPane.addCaretListener(listener);
    }

    private void queueDiff() {
        if (applyingHighlights) {
            return;
        }
        diffDebounceTimer.restart();
    }

    private void recomputeDiff() {
        currentResult = diffEngine.compare(leftPane.getText(), rightPane.getText());
        applyingHighlights = true;
        try {
            resetStyles(leftPane);
            resetStyles(rightPane);
            applyHighlights((StyledDocument) leftPane.getDocument(), currentResult.leftHighlights());
            applyHighlights((StyledDocument) rightPane.getDocument(), currentResult.rightHighlights());
        } finally {
            applyingHighlights = false;
        }
        overviewRuler.setDiffResult(currentResult);
        updateViewportIndicators();
        updateStatus(currentResult.stats());
        updateCurrentLineHighlights();
    }

    private void resetStyles(JTextPane pane) {
        StyledDocument document = pane.getStyledDocument();
        SimpleAttributeSet clear = new SimpleAttributeSet();
        StyleConstants.setBackground(clear, pane.getBackground());
        document.setCharacterAttributes(0, document.getLength(), clear, true);
    }

    private void applyHighlights(StyledDocument document, java.util.List<HighlightSpan> spans) {
        for (HighlightSpan span : spans) {
            if (span.endOffset() <= span.startOffset()) {
                continue;
            }
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setBackground(attributes, DiffColors.forType(span.type()));
            document.setCharacterAttributes(span.startOffset(), span.endOffset() - span.startOffset(), attributes, false);
        }
    }

    private void syncScrollFrom(JTextPane sourcePane, JTextPane targetPane) {
        if (syncingScroll || currentResult == null) {
            updateViewportIndicators();
            return;
        }
        syncingScroll = true;
        try {
            int sourceLine = getVisibleTopLine(sourcePane);
            int mappedLine = sourcePane == leftPane
                    ? mapLine(currentResult.leftToRightLineMap(), sourceLine)
                    : mapLine(currentResult.rightToLeftLineMap(), sourceLine);
            scrollPaneToLine(targetPane, mappedLine);
            updateViewportIndicators();
        } finally {
            syncingScroll = false;
        }
    }

    private void scrollToFraction(double fraction) {
        scrollPaneToFraction(leftPane, fraction);
        scrollPaneToFraction(rightPane, fraction);
        updateViewportIndicators();
    }

    private void scrollPaneToFraction(JTextPane pane, double fraction) {
        int lineCount = getLineCount(pane);
        if (lineCount == 0) {
            return;
        }
        int targetLine = Math.max(0, Math.min(lineCount - 1, (int) Math.round(fraction * Math.max(lineCount - 1, 0))));
        scrollPaneToLine(pane, targetLine);
    }

    private void scrollPaneToLine(JTextPane pane, int targetLine) {
        if (targetLine < 0 || getLineCount(pane) == 0) {
            return;
        }
        try {
            Rectangle rect = pane.modelToView2D(getLineStartOffset(pane, targetLine)).getBounds();
            pane.scrollRectToVisible(new Rectangle(0, rect.y, 1, pane.getVisibleRect().height));
        } catch (BadLocationException ignored) {
        }
    }

    private int mapLine(int[] mapping, int line) {
        if (mapping.length == 0) {
            return -1;
        }
        int clamped = Math.max(0, Math.min(line, mapping.length - 1));
        return mapping[clamped];
    }

    private int getVisibleTopLine(JTextPane pane) {
        Rectangle visible = pane.getVisibleRect();
        int offset = pane.viewToModel2D(visible.getLocation());
        Element root = pane.getDocument().getDefaultRootElement();
        return root.getElementIndex(Math.max(0, offset));
    }

    private int getVisibleBottomLine(JTextPane pane) {
        Rectangle visible = pane.getVisibleRect();
        int x = Math.max(0, visible.x);
        int y = Math.max(0, visible.y + Math.max(visible.height - 1, 0));
        int offset = pane.viewToModel2D(new Point(x, y));
        Element root = pane.getDocument().getDefaultRootElement();
        return root.getElementIndex(Math.max(0, offset));
    }

    private int getLineCount(JTextPane pane) {
        return pane.getDocument().getDefaultRootElement().getElementCount();
    }

    private int getLineStartOffset(JTextPane pane, int line) {
        Element root = pane.getDocument().getDefaultRootElement();
        int clamped = Math.max(0, Math.min(line, Math.max(root.getElementCount() - 1, 0)));
        return root.getElement(clamped).getStartOffset();
    }

    private void updateViewportIndicators() {
        overviewRuler.setViewportFractions(
                visibleStartFraction(leftPane),
                visibleEndFraction(leftPane),
                visibleStartFraction(rightPane),
                visibleEndFraction(rightPane)
        );
    }

    private double visibleStartFraction(JTextPane pane) {
        int lineCount = getLineCount(pane);
        if (lineCount == 0) {
            return 0.0;
        }
        return getVisibleTopLine(pane) / (double) Math.max(1, lineCount - 1);
    }

    private double visibleEndFraction(JTextPane pane) {
        int lineCount = getLineCount(pane);
        if (lineCount == 0) {
            return 1.0;
        }
        return getVisibleBottomLine(pane) / (double) Math.max(1, lineCount - 1);
    }

    private void updateStatus(DiffStats stats) {
        statusLabel.setText(String.format(
                "Added: %d   Removed: %d   Changed: %d",
                stats.addedLines(),
                stats.removedLines(),
                stats.changedLines()
        ));
    }

    private void refreshLineNumbers() {
        leftGutter.refresh();
        rightGutter.refresh();
    }

    private void updateCurrentLineHighlights() {
        JTextPane source = activePane != null ? activePane : leftPane;

        int sourceLine = getCaretLine(source);
        int targetLine = source == leftPane
                ? mapLine(currentResult.leftToRightLineMap(), sourceLine)
                : mapLine(currentResult.rightToLeftLineMap(), sourceLine);

        leftCurrentLineHighlight = replaceCurrentLineHighlight(
                leftPane,
                leftCurrentLineHighlight,
                source == leftPane ? sourceLine : targetLine
        );
        rightCurrentLineHighlight = replaceCurrentLineHighlight(
                rightPane,
                rightCurrentLineHighlight,
                source == rightPane ? sourceLine : targetLine
        );
    }

    private Object replaceCurrentLineHighlight(JTextPane pane, Object existingHighlight, int line) {
        Highlighter highlighter = pane.getHighlighter();
        if (existingHighlight != null) {
            highlighter.removeHighlight(existingHighlight);
        }
        if (line < 0 || getLineCount(pane) == 0) {
            return null;
        }
        try {
            Element root = pane.getDocument().getDefaultRootElement();
            int clamped = Math.max(0, Math.min(line, root.getElementCount() - 1));
            Element lineElement = root.getElement(clamped);
            int start = lineElement.getStartOffset();
            int end = Math.min(pane.getDocument().getLength(), Math.max(start + 1, lineElement.getEndOffset()));
            return highlighter.addHighlight(start, end, currentLinePainter);
        } catch (BadLocationException ignored) {
            return null;
        }
    }

    private int getCaretLine(JTextPane pane) {
        Element root = pane.getDocument().getDefaultRootElement();
        return root.getElementIndex(Math.max(0, pane.getCaretPosition()));
    }
}
