package com.baratali.difftool.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DiffEngineTest {
    private final DiffEngine engine = new DiffEngine();

    @Test
    void identicalTextDoesNotProduceDiffBlocks() {
        DiffResult identical = engine.compare("alpha\nbeta\n", "alpha\nbeta\n");

        assertTrue(identical.blocks().isEmpty());
    }

    @Test
    void countsAddedRemovedAndChangedLines() {
        DiffResult added = engine.compare("alpha\n", "alpha\nbeta\n");
        DiffResult removed = engine.compare("alpha\nbeta\n", "alpha\n");
        DiffResult changed = engine.compare("alpha beta\n", "alpha gamma\n");

        assertEquals(1, added.stats().addedLines());
        assertEquals(0, added.stats().removedLines());
        assertEquals(1, removed.stats().removedLines());
        assertEquals(1, changed.stats().changedLines());
        assertFalse(changed.leftHighlights().isEmpty());
        assertFalse(changed.rightHighlights().isEmpty());
    }

    @Test
    void alignedChangedLinesMapToEachOther() {
        DiffResult aligned = engine.compare("a\nb\nc\nd\n", "a\nx\nc\ny\n");

        assertEquals(0, aligned.leftToRightLineMap()[0]);
        assertEquals(1, aligned.leftToRightLineMap()[1]);
        assertEquals(2, aligned.leftToRightLineMap()[2]);
    }

    @Test
    void exactComparisonStillDetectsWhitespaceAndCaseChanges() {
        DiffResult whitespace = engine.compare("alpha beta\n", "alpha   beta\n");
        DiffResult casing = engine.compare("alpha\n", "ALPHA\n");

        assertEquals(1, whitespace.stats().changedLines());
        assertEquals(1, casing.stats().changedLines());
    }

    @Test
    void ignoreWhitespaceTrimsAndCollapsesSpacesAndTabs() {
        DiffOptions options = new DiffOptions(true, false, true);

        DiffResult result = engine.compare("  alpha\t\tbeta  \n", "alpha beta\n", options);

        assertTrue(result.blocks().isEmpty());
        assertEquals(0, result.stats().changedLines());
    }

    @Test
    void ignoreCaseSuppressesCaseOnlyDiffs() {
        DiffOptions options = new DiffOptions(false, true, true);

        DiffResult result = engine.compare("Alpha BETA\n", "alpha beta\n", options);

        assertTrue(result.blocks().isEmpty());
        assertEquals(0, result.stats().changedLines());
    }

    @Test
    void normalizesLineEndingsWhenEnabled() {
        DiffOptions options = new DiffOptions(false, false, true);

        DiffResult result = engine.compare("alpha\r\nbeta\r\n", "alpha\nbeta\n", options);

        assertTrue(result.blocks().isEmpty());
        assertEquals(0, result.stats().changedLines());
    }

    @Test
    void combinedOptionsStillDetectMeaningfulChanges() {
        DiffOptions options = new DiffOptions(true, true, true);

        DiffResult result = engine.compare("  Alpha beta\r\n", "alpha gamma\n", options);

        assertEquals(1, result.stats().changedLines());
        assertFalse(result.blocks().isEmpty());
        assertFalse(result.leftHighlights().isEmpty());
        assertFalse(result.rightHighlights().isEmpty());
    }
}
