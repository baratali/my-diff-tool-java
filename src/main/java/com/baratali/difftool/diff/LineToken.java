package com.baratali.difftool.diff;

public record LineToken(int lineIndex, String text, int startOffset, int endOffset) {
}
