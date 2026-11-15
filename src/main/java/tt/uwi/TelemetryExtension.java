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


public class TelemetryExtension extends Extension {

    private TelemetrySession session;
    private TelemetrySubject subject;
    private EditorListenerRegistry editorRegistry;
    private BackgroundExporter exporter;
    
    @Override 
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override 
    public String getName() {
        return "BlueJ Telemetry";
    }

    @Override 
    public String getVersion() {
        return "0.1";
    }

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

    private void exportCurrentSession() {
        TelemetrySession snapshot = session.snapshot(LocalDateTime.now());

        exporter.exportSnapshot(snapshot);

        System.out.println("Manual telemetry export queued; "
                            + "the session remains open."
        );
    }

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
