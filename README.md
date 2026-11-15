# COMP3607 GROUP 17 - BlueJ Telemetry Plugin

A Java extention for BlueJ that collects timestamped development
events and exports session summaries as JSON and PDF reports.

## Features

- Records package opening and closing.
- Detects class creation and deletion through class-list comparisons.
- Records class renames and compiled-state changes.
- Records compilation starts, outcomes, error messages and line numbers.
- Records completed invocations and their reported outcomes.
- Records editor document-change events.
- Estimates active and idle time using 30-second edit windows.
- Exports reports manually and during graceful shutdown.
- Detects method-signature additions and removals by comparing compiled
  class inventories.

## Technology

- Java and Maven
- BlueJ Extensions API
- Jackson for JSON export
- Apache PDFBox for PDF reports
- JUnit 5 and Mockito for testing
- JaCoCo for test coverage

-----------------------------------------------------------------------------------------------------------------------------------

## Build requirements

This porject was devloped and tested with

- BlueJ 6.0.1
- JDK 25
- Apache Maven 3.10.0

BlueJ uses its own bundled Java runtime. Building the plugin with Maven
also requires a JDK available in your terminal.

Check your development tools:

```powershell
java -version
javac -version
mvn -version
```

## Building the plugin

Run these commands from the project folder containing `pom.xml`.

First, install your local BlueJ library into Maven's local repository.
This is needed once on each development computer:

```powershell
mvn install:install-file "-Dfile=C:\Program Files\BlueJ\lib\bluej.jar" "-DgroupId=org.bluej" "-DartifactId=bluej" "-Dversion=6.0.1" "-Dpackaging=jar"
```

If BlueJ is installed elsewhere, change the file path accordingly.

Build the plugin and run its tests:

```powershell
mvn clean verify
```

The packaged plugin is created at:

```text
target/bluej-telemetry-plugin-1.0-SNAPSHOT.jar
```

The test coverage report is available at:

```text
target/site/jacoco/index.html
```

-----------------------------------------------------------------------------------------------------------------------------------

## Installing in BlueJ on Windows

Close all BlueJ instances before installing or replacing the plugin.

From the project folder, run:

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\bluej\extensions2" | Out-Null
Copy-Item ".\target\bluej-telemetry-plugin-1.0-SNAPSHOT.jar" "$env:USERPROFILE\bluej\extensions2\" -Force
```

Restart BlueJ. The plugin begins a new telemetry session when it loads.

## Using the plugin

Open a BlueJ project and work normally. Recorded events include
compilations, completed invocations and supported editor/class events.

To export the current session without closing BlueJ, select:

**Tools → Export telemetry report**

Manual export captures a snapshot. The live session continues collecting
events, so later exports include earlier events from the same session.

Closing BlueJ normally also requests a final export.

Reports are written to:

```text
C:\Users\<your-Windows-username>\bluej-telemetry-output
```

Each export produces a JSON file and a PDF file sharing the same
filename prefix: a project label, snapshot timestamp and unique ID.

For a session containing one project, the label is the project folder's
name, with unsupported filename characters replaced by underscores.
Sessions containing several projects use `multiple-projects`; sessions
without a registered project use `no-project`.

Report generation runs on a background worker, so files may take
a moment to appear.

The JSON contains event details, session metadata and registered project
directories. A session can contain events from several projects opened
in the same BlueJ instance.

The PDF contains compilation outcomes, activity estimates, error
frequencies and a class and method creation timeline.

## Measurement rules and limitations

- The student ID is currently `STUDENT-PLACEHOLDER`.
- Timestamps use the computer's local date and time.
- Package opening and closing are recorded; these do not represent
  every possible project lifecycle action.
- Class creation and deletion are detected by comparing class lists
  during scans. Their timestamps show detection time.
- Classes present at the initial scan form the baseline and are not
  counted as newly created.
- Editor activity counts document-change notifications, not individual
  keystrokes. Source text is not stored in telemetry events.
- Editors are attached during scans. Edits to a newly created class
  before its editor listener is attached may not be recorded.
- Each edit marks the following 30 seconds as estimated active time.
  Overlapping windows count once and stop at the snapshot's end time.
  Estimated idle time means time outside these windows, not proof
  that the user was inactive.
- Run counts represent completed invocations reported by BlueJ.
  Method names may be unavailable for some outcomes.
- Runtime exception occurrences are recorded without exception
  messages or stack traces.
- Console output, mouse movements, file navigation and activity
  outside BlueJ are not collected.
- Telemetry is held in memory. Unexpected termination can lose
  events that have not been exported.
- Graceful shutdown waits up to 30 seconds for queued exports.
  Export completion is not guaranteed if that wait expires.
- PDF reports are limited to two pages, showing up to five error
  messages and eighteen recorded class and method changes. Full recorded
  event details remain in the JSON.
- Method tracking compares compiled signatures, not method bodies.
  The first successful inspection establishes a baseline. Changes
  made before that baseline are not reported as additions or removals.

-----------------------------------------------------------------------------------------------------------------------------------

## Design patterns

### Observer

`TelemetrySubject` publishes events to registered observers.
`TelemetryCollector` receives these events and adds them to the session.

This separates event producers, such as BlueJ listeners, from event storage.

### Strategy

`ExportStrategy` defines a common export operation.
`JsonExportStrategy` and `PdfExportStrategy` implement the format-specific
behaviour, while `ExportService` delegates to the selected strategy.

This keeps JSON and PDF generation separate.

### Command

`JsonExportCommand` and `PdfExportCommand` represent export requests,
holding the export service, session snapshot and destination path.
`CommandInvoker` executes these commands.

`BackgroundExporter` uses these commands to produce both reports
on its worker thread.

-----------------------------------------------------------------------------------------------------------------------------------

## Testing

Run the automated tests:

```powershell
mvn test
```

Run a clean build, tests and coverage reporting:

```powershell
mvn clean verify
```

Tests cover session data, analytics, event handling and report export.
BlueJ API objects are mocked where needed, allowing the automated
tests to run without opening the BlueJ application.

For a manual integration check:

1. Open BlueJ and a test project.
2. Compile code successfully.
3. Introduce a syntax error and compile again.
4. Fix the error and recompile.
5. Select Tools → Export telemetry report.
6. Check that the JSON and PDF reflect the recorded outcomes.
7. Close BlueJ normally and check that a final report pair appears.

## Troubleshooting

### Maven cannot find a POM

Run Maven from the project folder containing `pom.xml`.

### Maven cannot resolve the BlueJ dependency

Follow the local BlueJ library installation command in the build
instructions. Confirm that the path to `bluej.jar` is correct.

### BlueJ appears to use an older plugin version

Close all BlueJ instances, rebuild the plugin, copy the updated JAR
into the extensions folder and restart BlueJ.

### Reports appear empty or unexpected

Check the session timestamps and the newest report files.
Separate BlueJ instances collect separate sessions.

### Reports do not appear

Allow time for background export, then inspect the BlueJ debug log:

```powershell
Get-Content "$env:USERPROFILE\bluej\bluej-debuglog.txt" -Tail 80
```

Look for export completion messages or errors.