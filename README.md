# Diff tool

Simple Java desktop diff tool built with Swing and Maven.

## Build

Build the project:

```bash
mvn package
```

This compiles the application and produces the jar in `target/`.

## Run

Run the desktop application with Maven:

```bash
mvn exec:java -Dexec.mainClass=com.baratali.difftool.DiffToolApp
```

You can also run the compiled jar directly after building:

```bash
java -cp target/my-diff-tool-java-1.0-SNAPSHOT.jar com.baratali.difftool.DiffToolApp
```

On macOS, if you launch the app this way, the Dock may still show `java` because the process is an unbundled JVM. The in-app title and menu name are set, but the Dock label is controlled by macOS application bundle metadata.

To get the correct Dock app name on macOS, build a native `.app` bundle with `jpackage`:

```bash
jpackage \
  --input target \
  --name "Diff Tool" \
  --main-jar my-diff-tool-java-1.0-SNAPSHOT.jar \
  --main-class com.baratali.difftool.DiffToolApp \
  --type app-image
```

This produces a native app bundle with the proper application name in the Dock.

## Tests

Run the Maven test phase:

```bash
mvn test
```
