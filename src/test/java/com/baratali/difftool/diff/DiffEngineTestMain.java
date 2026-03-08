package com.baratali.difftool.diff;

public final class DiffEngineTestMain {
    private DiffEngineTestMain() {
    }

    public static void main(String[] args) {
        DiffEngine engine = new DiffEngine();

        DiffResult identical = engine.compare("alpha\nbeta\n", "alpha\nbeta\n");
        assertTrue(identical.blocks().isEmpty(), "identical text should not produce diff blocks");

        DiffResult added = engine.compare("alpha\n", "alpha\nbeta\n");
        assertEquals(1, added.stats().addedLines(), "addition count should match");
        assertEquals(0, added.stats().removedLines(), "removal count should be zero");

        DiffResult removed = engine.compare("alpha\nbeta\n", "alpha\n");
        assertEquals(1, removed.stats().removedLines(), "removal count should match");

        DiffResult changed = engine.compare("alpha beta\n", "alpha gamma\n");
        assertEquals(1, changed.stats().changedLines(), "changed count should match");
        assertTrue(!changed.leftHighlights().isEmpty(), "changed line should produce left highlights");
        assertTrue(!changed.rightHighlights().isEmpty(), "changed line should produce right highlights");

        DiffResult aligned = engine.compare("a\nb\nc\nd\n", "a\nx\nc\ny\n");
        assertEquals(0, aligned.leftToRightLineMap()[0], "first unchanged line should map directly");
        assertEquals(1, aligned.leftToRightLineMap()[1], "changed lines should align to each other");
        assertEquals(2, aligned.leftToRightLineMap()[2], "unchanged lines should map directly");

        System.out.println("DiffEngine tests passed.");
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " Expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
