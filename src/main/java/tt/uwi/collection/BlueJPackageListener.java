package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.BPackage;
import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

public class BlueJPackageListener implements PackageListener {

    private final TelemetrySubject subject;
    private final BlueJClassScanner scanner;

    public BlueJPackageListener(TelemetrySubject subject, BlueJClassScanner scanner) {
        this.subject = subject;
        this.scanner = scanner;
    }

    @Override
    public void packageOpened(PackageEvent event) {
        recordPackageEvent(event, EventType.PACKAGE_OPENED);
        scanner.scanPackage(event.getPackage());
    }

    @Override
    public void packageClosing(PackageEvent event) {
        scanner.scanPackage(event.getPackage());
        recordPackageEvent(event, EventType.PACKAGE_CLOSING);
        scanner.forgetPackage(event.getPackage());
    }

    private void recordPackageEvent(
            PackageEvent event, EventType type) {

        LocalDateTime timestamp = LocalDateTime.now();
        String description;

        try {
            BPackage bluejPackage = event.getPackage();

            String packageName = bluejPackage.getName();
            String projectPath =
                    bluejPackage.getProject().getDir().getAbsolutePath();

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