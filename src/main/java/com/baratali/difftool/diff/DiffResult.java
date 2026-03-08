package com.baratali.difftool.diff;

import java.util.List;

public record DiffResult(
        List<HighlightSpan> leftHighlights,
        List<HighlightSpan> rightHighlights,
        List<DiffBlock> blocks,
        int[] leftToRightLineMap,
        int[] rightToLeftLineMap,
        int leftLineCount,
        int rightLineCount,
        DiffStats stats
) {
}
