package tt.uwi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import tt.uwi.analytics.AnalyticsSummary;
import tt.uwi.analytics.SessionAnalytics;
import tt.uwi.collection.TelemetryCollector;
import tt.uwi.collection.TelemetrySubject;
import tt.uwi.commands.CommandInvoker;
import tt.uwi.commands.JsonExportCommand;
import tt.uwi.commands.PdfExportCommand;
import tt.uwi.export.ExportService;
import tt.uwi.export.JsonExportStrategy;
import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

/**
 * Demonstrates telemetry collection, summary calculation, and JSON/PDF export
 * with sample events outside a running BlueJ extension.
 */
public class DemoMain {

    /**
     * Runs the telemetry demonstration from a live in-memory session.
     * Publishes sample events through the observer subject, prints session
     * statistics, creates the output directory, and exports JSON and PDF reports.
     *
     * @param args command-line arguments; this demo does not require any
     * @throws Exception if output directories or export files cannot be created
     */
    public static void main(String[] args) throws Exception {

        TelemetrySession session =
                new TelemetrySession("STUDENT-001", LocalDateTime.now());

        TelemetryCollector collector =
                new TelemetryCollector(session);

        // Observer pattern: register the collector.
        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(collector);

        // Sample events for testing before BlueJ integration.
        subject.publish(new TelemetryEvent(
                EventType.PROJECT_OPENED,
                LocalDateTime.now(),
                "Demo project opened",
                null
        ));

        subject.publish(new TelemetryEvent(
                EventType.CLASS_CREATED,
                LocalDateTime.now(),
                "Created a class",
                "Student"
        ));

        subject.publish(new TelemetryEvent(
                EventType.COMPILE_SUCCESS,
                LocalDateTime.now(),
                "Compilation succeeded",
                "Student"
        ));

        subject.publish(new TelemetryEvent(
                EventType.PROGRAM_RUN,
                LocalDateTime.now(),
                "Program executed",
                "Student"
        ));

        subject.publish(new TelemetryEvent(
                EventType.COMPILE_FAILURE,
                LocalDateTime.now(),
                "Example compile error",
                "Student"
        ));

        session.setEndTime(LocalDateTime.now());

        // Calculate session statistics.
        AnalyticsSummary summary =
                new SessionAnalytics().analyse(session);

        System.out.println("Events recorded: " +
                summary.getTotalEvents());

        System.out.println("Compilation attempts: " +
                summary.getCompilationAttempts());

        System.out.println("Compilation failures: " +
                summary.getCompilationFailures());
        
        System.out.println("Compilation success rate: " + 
                summary.getCompilationSuccessRate() + "%");

        // Create the output folder.
        Path outputDirectory = Path.of("telemetry-output");
        Files.createDirectories(outputDirectory);

        // Command and Strategy patterns: perform both exports.
        ExportService exportService =
                new ExportService(new JsonExportStrategy());

        CommandInvoker invoker = new CommandInvoker();

        invoker.run(new JsonExportCommand(
                exportService,
                session,
                outputDirectory.resolve("session.json")
        ));

        invoker.run(new PdfExportCommand(
                exportService,
                session,
                outputDirectory.resolve("session-report.pdf")
        ));

        System.out.println("Files written to: " +
                outputDirectory.toAbsolutePath());
    }
}
