package com.baratali.difftool.diff;

public record LineToken(int lineIndex, String text, String comparisonText, int startOffset, int endOffset) {
    public LineToken(int lineIndex, String text, int startOffset, int endOffset) {
        this(lineIndex, text, text, startOffset, endOffset);
    }
}
