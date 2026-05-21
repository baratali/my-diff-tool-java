package com.baratali.difftool.diff;

public record DiffOptions(boolean ignoreWhitespace, boolean ignoreCase, boolean normalizeLineEndings) {
    public static DiffOptions exact() {
        return new DiffOptions(false, false, false);
    }
}
