package com.baratali.difftool.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiffEngine {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\S+|\\s+");

    public DiffResult compare(String leftText, String rightText) {
        List<LineToken> leftLines = splitLines(leftText);
        List<LineToken> rightLines = splitLines(rightText);

        List<LineEdit> edits = buildLineEdits(leftLines, rightLines);
        int[] leftMap = new int[leftLines.size()];
        int[] rightMap = new int[rightLines.size()];
        for (int i = 0; i < leftMap.length; i++) {
            leftMap[i] = rightLines.isEmpty() ? -1 : 0;
        }
        for (int i = 0; i < rightMap.length; i++) {
            rightMap[i] = leftLines.isEmpty() ? -1 : 0;
        }

        List<HighlightSpan> leftHighlights = new ArrayList<>();
        List<HighlightSpan> rightHighlights = new ArrayList<>();
        List<DiffBlock> blocks = new ArrayList<>();
        int addedLines = 0;
        int removedLines = 0;
        int changedLines = 0;

        int sharedCursor = 0;
        for (LineEdit edit : edits) {
            int leftCount = Math.max(0, edit.leftEnd - edit.leftStart);
            int rightCount = Math.max(0, edit.rightEnd - edit.rightStart);
            int blockSize = Math.max(leftCount, rightCount);
            int blockStart = sharedCursor;
            int blockEnd = sharedCursor + Math.max(blockSize, 1);
            sharedCursor = blockEnd;

            switch (edit.type) {
                case UNCHANGED -> mapEqualRange(leftMap, rightMap, edit.leftStart, edit.rightStart, leftCount);
                case REMOVED -> {
                    removedLines += leftCount;
                    fillMapping(leftMap, edit.leftStart, edit.leftEnd, clampIndex(edit.rightStart, rightLines.size()));
                    addWholeLineHighlights(leftHighlights, leftLines, edit.leftStart, edit.leftEnd, DiffType.REMOVED);
                }
                case ADDED -> {
                    addedLines += rightCount;
                    fillMapping(rightMap, edit.rightStart, edit.rightEnd, clampIndex(edit.leftStart, leftLines.size()));
                    addWholeLineHighlights(rightHighlights, rightLines, edit.rightStart, edit.rightEnd, DiffType.ADDED);
                }
                case CHANGED -> {
                    changedLines += Math.max(leftCount, rightCount);
                    mapChangedRange(leftMap, rightMap, edit.leftStart, edit.leftEnd, edit.rightStart, edit.rightEnd);
                    addChangedHighlights(leftHighlights, rightHighlights, leftLines, rightLines, edit);
                }
            }

            if (edit.type != DiffType.UNCHANGED) {
                blocks.add(new DiffBlock(
                        edit.type,
                        edit.leftStart,
                        edit.leftEnd,
                        edit.rightStart,
                        edit.rightEnd,
                        0.0,
                        0.0
                ));
            }
        }

        if (!leftLines.isEmpty() && !rightLines.isEmpty()) {
            fillUnsetMappings(leftMap, rightMap, leftLines.size(), rightLines.size());
        }

        List<DiffBlock> normalizedBlocks = normalizeBlocks(blocks, leftLines.size(), rightLines.size());
        return new DiffResult(
                leftHighlights,
                rightHighlights,
                normalizedBlocks,
                leftMap,
                rightMap,
                leftLines.size(),
                rightLines.size(),
                new DiffStats(addedLines, removedLines, changedLines)
        );
    }

    private List<DiffBlock> normalizeBlocks(List<DiffBlock> blocks, int leftLineCount, int rightLineCount) {
        if (blocks.isEmpty()) {
            return blocks;
        }
        double divisor = Math.max(Math.max(leftLineCount, rightLineCount), 1);
        List<DiffBlock> normalized = new ArrayList<>(blocks.size());
        for (DiffBlock block : blocks) {
            int startLine = Math.max(block.leftStartLine(), block.rightStartLine());
            int endLine = Math.max(block.leftEndLine(), block.rightEndLine());
            if (endLine <= startLine) {
                endLine = startLine + 1;
            }
            double start = startLine / divisor;
            double end = endLine / divisor;
            normalized.add(new DiffBlock(
                    block.type(),
                    block.leftStartLine(),
                    block.leftEndLine(),
                    block.rightStartLine(),
                    block.rightEndLine(),
                    start,
                    Math.min(1.0, end)
            ));
        }
        return normalized;
    }

    private void addChangedHighlights(
            List<HighlightSpan> leftHighlights,
            List<HighlightSpan> rightHighlights,
            List<LineToken> leftLines,
            List<LineToken> rightLines,
            LineEdit edit
    ) {
        int pairCount = Math.min(edit.leftEnd - edit.leftStart, edit.rightEnd - edit.rightStart);
        for (int i = 0; i < pairCount; i++) {
            LineToken left = leftLines.get(edit.leftStart + i);
            LineToken right = rightLines.get(edit.rightStart + i);

            if (left.text().equals(right.text())) {
                continue;
            }

            List<TokenSpan> leftDiffs = tokenDiff(left.text(), right.text(), left.startOffset(), true);
            List<TokenSpan> rightDiffs = tokenDiff(left.text(), right.text(), right.startOffset(), false);

            if (leftDiffs.isEmpty()) {
                leftHighlights.add(new HighlightSpan(left.startOffset(), left.endOffset(), DiffType.CHANGED));
            } else {
                for (TokenSpan span : leftDiffs) {
                    leftHighlights.add(new HighlightSpan(span.startOffset(), span.endOffset(), DiffType.CHANGED));
                }
            }

            if (rightDiffs.isEmpty()) {
                rightHighlights.add(new HighlightSpan(right.startOffset(), right.endOffset(), DiffType.CHANGED));
            } else {
                for (TokenSpan span : rightDiffs) {
                    rightHighlights.add(new HighlightSpan(span.startOffset(), span.endOffset(), DiffType.CHANGED));
                }
            }
        }

        if (edit.leftEnd - edit.leftStart > pairCount) {
            addWholeLineHighlights(leftHighlights, leftLines, edit.leftStart + pairCount, edit.leftEnd, DiffType.REMOVED);
        }
        if (edit.rightEnd - edit.rightStart > pairCount) {
            addWholeLineHighlights(rightHighlights, rightLines, edit.rightStart + pairCount, edit.rightEnd, DiffType.ADDED);
        }
    }

    private List<TokenSpan> tokenDiff(String leftText, String rightText, int baseOffset, boolean fromLeft) {
        List<Token> sourceTokens = tokenizeInline(fromLeft ? leftText : rightText, baseOffset);
        List<Token> otherTokens = tokenizeInline(fromLeft ? rightText : leftText, 0);
        if (sourceTokens.isEmpty()) {
            return Collections.emptyList();
        }

        int[][] dp = buildLcsTable(sourceTokens.stream().map(Token::value).toList(),
                otherTokens.stream().map(Token::value).toList());
        List<TokenSpan> spans = new ArrayList<>();
        int i = 0;
        int j = 0;
        Integer pendingStart = null;
        int pendingEnd = -1;
        while (i < sourceTokens.size() && j < otherTokens.size()) {
            String source = sourceTokens.get(i).value();
            String other = otherTokens.get(j).value();
            if (source.equals(other)) {
                if (pendingStart != null) {
                    spans.add(new TokenSpan(pendingStart, pendingEnd));
                    pendingStart = null;
                }
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                Token token = sourceTokens.get(i++);
                if (!token.value().isBlank()) {
                    if (pendingStart == null) {
                        pendingStart = token.startOffset();
                    }
                    pendingEnd = token.endOffset();
                }
            } else {
                j++;
            }
        }

        while (i < sourceTokens.size()) {
            Token token = sourceTokens.get(i++);
            if (!token.value().isBlank()) {
                if (pendingStart == null) {
                    pendingStart = token.startOffset();
                }
                pendingEnd = token.endOffset();
            }
        }

        if (pendingStart != null) {
            spans.add(new TokenSpan(pendingStart, pendingEnd));
        }
        return spans;
    }

    private void mapEqualRange(int[] leftMap, int[] rightMap, int leftStart, int rightStart, int count) {
        for (int i = 0; i < count; i++) {
            leftMap[leftStart + i] = rightStart + i;
            rightMap[rightStart + i] = leftStart + i;
        }
    }

    private void mapChangedRange(
            int[] leftMap,
            int[] rightMap,
            int leftStart,
            int leftEnd,
            int rightStart,
            int rightEnd
    ) {
        int leftCount = leftEnd - leftStart;
        int rightCount = rightEnd - rightStart;
        for (int i = 0; i < leftCount; i++) {
            double ratio = leftCount <= 1 ? 0.0 : (double) i / (leftCount - 1);
            int target = rightCount == 0 ? rightStart : rightStart + (int) Math.round(ratio * Math.max(rightCount - 1, 0));
            leftMap[leftStart + i] = target;
        }
        for (int i = 0; i < rightCount; i++) {
            double ratio = rightCount <= 1 ? 0.0 : (double) i / (rightCount - 1);
            int target = leftCount == 0 ? leftStart : leftStart + (int) Math.round(ratio * Math.max(leftCount - 1, 0));
            rightMap[rightStart + i] = target;
        }
    }

    private void fillMapping(int[] mapping, int start, int end, int value) {
        for (int i = start; i < end; i++) {
            mapping[i] = value;
        }
    }

    private void fillUnsetMappings(int[] leftMap, int[] rightMap, int leftLineCount, int rightLineCount) {
        for (int i = 0; i < leftMap.length; i++) {
            leftMap[i] = clampIndex(leftMap[i], rightLineCount);
        }
        for (int i = 0; i < rightMap.length; i++) {
            rightMap[i] = clampIndex(rightMap[i], leftLineCount);
        }
    }

    private int clampIndex(int index, int size) {
        if (size == 0) {
            return -1;
        }
        if (index < 0) {
            return 0;
        }
        return Math.min(index, size - 1);
    }

    private void addWholeLineHighlights(
            List<HighlightSpan> target,
            List<LineToken> lines,
            int start,
            int end,
            DiffType type
    ) {
        for (int i = start; i < end; i++) {
            LineToken line = lines.get(i);
            target.add(new HighlightSpan(line.startOffset(), line.endOffset(), type));
        }
    }

    private List<LineEdit> buildLineEdits(List<LineToken> leftLines, List<LineToken> rightLines) {
        List<String> leftTexts = leftLines.stream().map(LineToken::text).toList();
        List<String> rightTexts = rightLines.stream().map(LineToken::text).toList();
        int[][] dp = buildLcsTable(leftTexts, rightTexts);
        List<RawEdit> rawEdits = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < leftTexts.size() && j < rightTexts.size()) {
            if (leftTexts.get(i).equals(rightTexts.get(j))) {
                rawEdits.add(new RawEdit(DiffType.UNCHANGED, i, i + 1, j, j + 1));
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                rawEdits.add(new RawEdit(DiffType.REMOVED, i, i + 1, j, j));
                i++;
            } else {
                rawEdits.add(new RawEdit(DiffType.ADDED, i, i, j, j + 1));
                j++;
            }
        }
        while (i < leftTexts.size()) {
            rawEdits.add(new RawEdit(DiffType.REMOVED, i, i + 1, j, j));
            i++;
        }
        while (j < rightTexts.size()) {
            rawEdits.add(new RawEdit(DiffType.ADDED, i, i, j, j + 1));
            j++;
        }

        List<RawEdit> mergedRawEdits = mergeAdjacent(rawEdits);
        List<LineEdit> edits = new ArrayList<>();
        int index = 0;
        while (index < mergedRawEdits.size()) {
            RawEdit current = mergedRawEdits.get(index);
            if (current.type == DiffType.REMOVED
                    && index + 1 < mergedRawEdits.size()
                    && mergedRawEdits.get(index + 1).type == DiffType.ADDED) {
                RawEdit added = mergedRawEdits.get(index + 1);
                edits.add(new LineEdit(DiffType.CHANGED, current.leftStart, current.leftEnd, added.rightStart, added.rightEnd));
                index += 2;
                continue;
            }
            if (current.type == DiffType.ADDED
                    && index + 1 < mergedRawEdits.size()
                    && mergedRawEdits.get(index + 1).type == DiffType.REMOVED) {
                RawEdit removed = mergedRawEdits.get(index + 1);
                edits.add(new LineEdit(DiffType.CHANGED, removed.leftStart, removed.leftEnd, current.rightStart, current.rightEnd));
                index += 2;
                continue;
            }
            edits.add(new LineEdit(current.type, current.leftStart, current.leftEnd, current.rightStart, current.rightEnd));
            index++;
        }
        return edits;
    }

    private List<RawEdit> mergeAdjacent(List<RawEdit> rawEdits) {
        if (rawEdits.isEmpty()) {
            return rawEdits;
        }
        List<RawEdit> merged = new ArrayList<>();
        RawEdit current = rawEdits.get(0);
        for (int i = 1; i < rawEdits.size(); i++) {
            RawEdit next = rawEdits.get(i);
            if (current.type == next.type
                    && current.leftEnd == next.leftStart
                    && current.rightEnd == next.rightStart) {
                current = new RawEdit(current.type, current.leftStart, next.leftEnd, current.rightStart, next.rightEnd);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private int[][] buildLcsTable(List<String> left, List<String> right) {
        int[][] dp = new int[left.size() + 1][right.size() + 1];
        for (int i = left.size() - 1; i >= 0; i--) {
            for (int j = right.size() - 1; j >= 0; j--) {
                if (left.get(i).equals(right.get(j))) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        return dp;
    }

    private List<LineToken> splitLines(String text) {
        List<LineToken> lines = new ArrayList<>();
        if (text.isEmpty()) {
            return lines;
        }
        int start = 0;
        int lineIndex = 0;
        while (start < text.length()) {
            int newline = text.indexOf('\n', start);
            int end = newline >= 0 ? newline + 1 : text.length();
            lines.add(new LineToken(lineIndex++, text.substring(start, end), start, end));
            start = end;
        }
        if (text.endsWith("\n")) {
            lines.add(new LineToken(lineIndex, "", text.length(), text.length()));
        }
        return lines;
    }

    private List<Token> tokenizeInline(String text, int baseOffset) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(new Token(matcher.group(), baseOffset + matcher.start(), baseOffset + matcher.end()));
        }
        return tokens;
    }

    private record RawEdit(DiffType type, int leftStart, int leftEnd, int rightStart, int rightEnd) {
    }

    private record LineEdit(DiffType type, int leftStart, int leftEnd, int rightStart, int rightEnd) {
    }

    private record Token(String value, int startOffset, int endOffset) {
    }

    private record TokenSpan(int startOffset, int endOffset) {
    }
}
