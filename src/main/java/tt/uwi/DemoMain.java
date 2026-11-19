package tt.uwi;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.ProjectNotOpenException;

import java.time.LocalDateTime;

import tt.uwi.collection.TelemetryCollector;
import tt.uwi.collection.TelemetrySubject;
import tt.uwi.telemetry.model.TelemetrySession;

import tt.uwi.collection.BlueJCompileListener;
import tt.uwi.collection.BlueJPackageListener;
import tt.uwi.collection.ClassInventoryTracker;
import tt.uwi.collection.BlueJClassScanner;
import tt.uwi.analytics.AnalyticsSummary;
import tt.uwi.analytics.SessionAnalytics;
import tt.uwi.collection.BlueJClassListener;
import tt.uwi.collection.BlueJExecutionListener;
import tt.uwi.collection.EditorListenerRegistry;
import tt.uwi.collection.MethodInventoryTracker;
import tt.uwi.collection.BlueJMethodScanner;

import java.nio.file.Path;
import tt.uwi.export.BackgroundExporter;

/**
 * BlueJ extension entry point that collects telemetry, registers BlueJ event
 * listeners, provides manual export, and performs a final export on shutdown.
 */
public class TelemetryExtension extends Extension {

    /** The live telemetry session maintained by the extension. */
    private TelemetrySession session;

    /** Subject used to publish telemetry events to registered observers. */
    private TelemetrySubject subject;

    /** Registry for editor listeners attached to scanned classes. */
    private EditorListenerRegistry editorRegistry;

    /** Background worker used for JSON and PDF exports. */
    private BackgroundExporter exporter;

    /**
     * Checks whether the extension API version is supported.
     *
     * @return {@code true} when the extension API major version is at least 3
     */
    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    /** @return the extension display name */
    @Override
    public String getName() {
        return "BlueJ Telemetry";
    }

    /** @return the extension version string */
    @Override
    public String getVersion() {
        return "0.1";
    }

    /**
     * Starts the telemetry session and registers all BlueJ listeners and the
     * manual-export menu action.
     *
     * @param bluej the active BlueJ environment
     */
    @Override
    public void startup(BlueJ bluej) {
        session = new TelemetrySession(
                "STUDENT-PLACEHOLDER",
                LocalDateTime.now()
        );

        exporter = new BackgroundExporter(
                Path.of(System.getProperty("user.home"), 
                        "bluej-telemetry-output"
                )
        );

        subject = new TelemetrySubject();
        TelemetryCollector collector = new TelemetryCollector(session);
        subject.addObserver(collector);
        ClassInventoryTracker tracker = new ClassInventoryTracker(subject);

        MethodInventoryTracker methodTracker = new MethodInventoryTracker(subject);
        BlueJMethodScanner methodScanner = new BlueJMethodScanner(bluej, methodTracker);

        editorRegistry = new EditorListenerRegistry(subject);

        BlueJClassScanner scanner = new BlueJClassScanner(bluej, tracker, editorRegistry);

        for (var project : bluej.getOpenProjects()) {
            try {
                session.registerProject(project.getDir().getAbsolutePath());
            } catch (ProjectNotOpenException exception) {
                System.err.println("Could not register an open project: "
                                        + exception.getMessage()
                );
            }
        }

        scanner.scanOpenProjects();
        methodScanner.scanOpenProjects();

        bluej.addCompileListener(
            new BlueJCompileListener(
                subject,
                scanner::scanOpenProjects,
                methodScanner::scanOpenProjects
            )
        );

        bluej.addPackageListener(
            new BlueJPackageListener(
                subject, 
                scanner, 
                session::registerProject,
                methodScanner::scanPackage,
                methodTracker::forgetPackage
            )
        );

        bluej.addClassListener(
            new BlueJClassListener(tracker, subject)
        );

        bluej.addInvocationFinishedListener(
            new BlueJExecutionListener(subject)
        );

        System.out.println("Telemetry session started.");

        bluej.setMenuGenerator(new TelemetryMenu(this::exportCurrentSession));
    }

    /**
     * Creates a snapshot of the live session and queues its JSON and PDF export.
     * The live session continues collecting events after the snapshot.
     */
    private void exportCurrentSession() {
        TelemetrySession snapshot = session.snapshot(LocalDateTime.now());

        exporter.exportSnapshot(snapshot);

        System.out.println("Manual telemetry export queued; "
                            + "the session remains open."
        );
    }

    /**
     * Stops editor listening, creates and exports a final session snapshot,
     * prints summary compilation counts, and closes the background exporter.
     * Editor detachment and exporter shutdown are attempted even when parts of
     * the session state are unavailable.
     */
    @Override
    public void terminate() {
        if (editorRegistry != null) {
            editorRegistry.detachAll();
        }

        if (exporter == null) {
            return;
        }

        try {
            if (session == null) {
                return;
            }

            LocalDateTime endedAt = LocalDateTime.now();
            session.setEndTime(endedAt);

            TelemetrySession snapshot = session.snapshot(endedAt);

            AnalyticsSummary summary =
                    new SessionAnalytics().analyse(snapshot);

            System.out.println("Telemetry session ended.");
            System.out.println("Compilation attempts: "
                    + summary.getCompilationAttempts());
            System.out.println("Compilation failures: "
                    + summary.getCompilationFailures());

            exporter.exportSnapshot(snapshot);

        } finally {
            exporter.close();
        }
    }
}
