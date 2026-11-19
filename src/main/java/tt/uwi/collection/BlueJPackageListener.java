package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.BPackage;
import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

import java.util.function.Consumer;
/**
 * Receives BlueJ package lifecycle events and coordinates inventory scans,
 * method cleanup, and package telemetry publication.
 */

public class BlueJPackageListener implements PackageListener {

    /** Publishes package lifecycle telemetry to registered observers. */
    private final TelemetrySubject subject;

    /** Scanner used to synchronize package class inventory. */
    private final BlueJClassScanner scanner;

    /** Callback that registers an open project path. */
    private final Consumer<String> registerProject;

    /** Callback that scans methods for an opened package. */
    private final Consumer<BPackage> scanMethods;

    /** Callback that removes method inventory for a closing package. */
    private final Consumer<String> forgetMethods;

    /**
     * Constructs a package listener with project and method callbacks disabled.
     *
     * @param subject the subject used to publish telemetry
     * @param scanner the class scanner used for package lifecycle updates
     */
    public BlueJPackageListener(TelemetrySubject subject, BlueJClassScanner scanner) {

        this(subject, scanner, directory -> {});
    }

    /**
     * Constructs a package listener with project registration enabled.
     * Method scanning and cleanup callbacks remain no-op defaults.
     *
     * @param subject the subject used to publish telemetry
     * @param scanner the class scanner used for package lifecycle updates
     * @param registerProject callback that registers an open project path
     */
    public BlueJPackageListener(
        TelemetrySubject subject,
        BlueJClassScanner scanner,
        Consumer<String> registerProject) {

        this(subject, scanner, registerProject,
                bluejPackage -> {}, directory -> {});
    }

    /**
     * Constructs a package listener with all lifecycle callbacks.
     *
     * @param subject the subject used to publish telemetry
     * @param scanner the class scanner used for package lifecycle updates
     * @param registerProject callback that registers an open project path
     * @param scanMethods callback that scans methods for an opened package
     * @param forgetMethods callback that removes method state for a package
     */
    public BlueJPackageListener(
            TelemetrySubject subject,
            BlueJClassScanner scanner,
            Consumer<String> registerProject,
            Consumer<BPackage> scanMethods,
            Consumer<String> forgetMethods) {

        this.subject = subject;
        this.scanner = scanner;
        this.registerProject = registerProject;
        this.scanMethods = scanMethods;
        this.forgetMethods = forgetMethods;
    }

    /**
     * Handles a package-opened notification by publishing its details and
     * scanning its classes and methods.
     *
     * @param event the package-opened event
     */
    @Override
    public void packageOpened(PackageEvent event) {
        recordPackageEvent(event, EventType.PACKAGE_OPENED);
        scanner.scanPackage(event.getPackage());
        scanMethods.accept(event.getPackage());
    }

    /**
     * Handles a package-closing notification. The package is scanned once
     * before its package event is published and its stored state is removed.
     *
     * @param event the package-closing event
     */
    @Override
    public void packageClosing(PackageEvent event) {
        scanner.scanPackage(event.getPackage());
        recordPackageEvent(event, EventType.PACKAGE_CLOSING);
        scanner.forgetPackage(event.getPackage());
    }

    /**
     * Builds and publishes a package lifecycle telemetry event.
     * Closing events also clear tracked method state for the package directory.
     *
     * @param event the package notification
     * @param type the telemetry event type to publish
     */
    private void recordPackageEvent(
            PackageEvent event, EventType type) {

        LocalDateTime timestamp = LocalDateTime.now();
        String description;

        try {
            BPackage bluejPackage = event.getPackage();

            String packageName = bluejPackage.getName();
            String projectPath = bluejPackage.getProject().getDir().getAbsolutePath();

            registerProject.accept(projectPath);

            if (type == EventType.PACKAGE_CLOSING) {
                forgetMethods.accept(bluejPackage.getDir().getAbsolutePath());
            }

            if (packageName.isEmpty()) {
                packageName = "(default package)";
            }

            description = "Project: " + projectPath
                    + " | Package: " + packageName;

        } catch (ProjectNotOpenException | PackageNotFoundException exception) {
            description = "Package notification received; details unavailable.";

            System.err.println(
                    "Could not read package details: "
                    + exception.getMessage()
            );
        }

        subject.publish(new TelemetryEvent(
                type,
                timestamp,
                description,
                null
        ));
    }
}
