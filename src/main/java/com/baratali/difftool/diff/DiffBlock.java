package com.baratali.difftool.diff;

public record DiffBlock(
        DiffType type,
        int leftStartLine,
        int leftEndLine,
        int rightStartLine,
        int rightEndLine,
        double overviewStart,
        double overviewEnd
) {
}
