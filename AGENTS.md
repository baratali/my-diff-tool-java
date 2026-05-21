# AGENTS.md

Guidance for automated coding agents working in this repository.

## Project Overview

This is a Java 17 desktop diff tool built with Swing and Maven.

- Entry point: `src/main/java/com/baratali/difftool/DiffToolApp.java`
- Swing UI: `src/main/java/com/baratali/difftool/ui/`
- Diff model and algorithm: `src/main/java/com/baratali/difftool/diff/`
- Tests: `src/test/java/com/baratali/difftool/diff/`
- Build metadata: `pom.xml`

The UI lets users paste original and modified text, compares them with configurable options, highlights changed spans, synchronizes scrolling, and shows an overview ruler plus diff statistics.

## Build And Test

Use the system Maven installation; this repo does not include a Maven wrapper.

```bash
mvn test
mvn package
mvn exec:java -Dexec.mainClass=com.baratali.difftool.DiffToolApp
```

Run `mvn test` before handing off changes that touch `src/main/java` or `src/test/java`. Use `mvn package` when validating packaging or the runnable jar.

## Repository Conventions

- Keep source compatible with Java 17, as configured by `maven.compiler.release`.
- Follow the existing package layout under `com.baratali.difftool`.
- Keep diff computation in `diff` classes and Swing-specific behavior in `ui` classes.
- Prefer immutable records/value types for simple data carriers, matching the existing diff model.
- Keep UI work on the Swing event dispatch thread. The app entry point already uses `SwingUtilities.invokeLater`.
- Use standard Swing/AWT APIs; do not add UI frameworks unless the task explicitly requires it.
- Avoid committing generated or local IDE/build output. Existing generated folders include `target/`, `out/`, `out-test/`, `.idea/`, and the macOS `Diff Tool.app` bundle.

## Diff Engine Notes

`DiffEngine` is the core behavior and should stay UI-independent.

- It compares line tokens first, then computes inline token spans for changed lines.
- `DiffOptions` controls whitespace handling, case handling, and line-ending normalization.
- `DiffResult` includes highlights, normalized overview blocks, line mappings, line counts, and `DiffStats`.
- When changing comparison behavior, add or update focused tests in `DiffEngineTest`.
- Be careful with line offsets: highlight spans are document offsets used directly by the Swing text panes.

## UI Notes

`DiffToolFrame` owns the main application window and coordinates editors, gutters, overview ruler, menu actions, scroll sync, undo/redo, zoom, and diff recomputation.

- Preserve the debounce behavior around diff recomputation unless a task specifically targets responsiveness.
- Keep changes to colors centralized in `DiffColors` where possible.
- If changing rendering behavior, check related custom components such as `LineNumberGutter` and `OverviewRuler`.
- Manual visual testing is useful for UI changes: run the app with `mvn exec:java -Dexec.mainClass=com.baratali.difftool.DiffToolApp`.

## Testing Expectations

- Add unit tests for diff algorithm changes.
- Prefer deterministic text fixtures inside tests instead of external files.
- Current automated tests focus on `DiffEngine`; Swing behavior may require manual validation.
- For bug fixes, include a regression test when the issue is in pure diff logic.

## Editing Guidelines

- Keep changes narrowly scoped to the requested behavior.
- Do not reformat unrelated files or churn imports without need.
- Preserve user-facing README commands unless the build process changes.
- Do not delete existing screenshots or app bundle artifacts unless explicitly asked.
