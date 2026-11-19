package tt.uwi.collection;

import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;

import java.time.LocalDateTime;
import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Receives BlueJ class events and updates the class inventory or publishes
 * telemetry for class state changes.
 */

public class BlueJClassListener implements ClassListener {

    /** Tracks class names and publishes inventory-change telemetry. */
    private final ClassInventoryTracker tracker;

    /** Publishes telemetry events for class state changes. */
    private final TelemetrySubject subject;

    /**
     * Constructs a listener with the tracker and telemetry subject to use.
     *
     * @param tracker the class inventory tracker
     * @param subject the subject that publishes telemetry events
     */
    public BlueJClassListener(ClassInventoryTracker tracker, TelemetrySubject subject) {
        this.tracker = tracker;
        this.subject = subject;
    }

    /**
     * Handles a BlueJ class rename notification.
     * The package directory is used as the inventory key for the class.
     *
     * @param event the class event containing the old and new class names
     */
    @Override
    public void classNameChanged(ClassEvent event) {
        try {
            String packageKey =
                    event.getPackage().getDir().getAbsolutePath();

            String oldName = event.getOldName();
            String newName = event.getBClass().getName();

            tracker.recordRename(packageKey, oldName, newName);

        } catch (ProjectNotOpenException |
                 PackageNotFoundException exception) {
            System.err.println(
                    "Could not record class rename: "
                    + exception.getMessage()
            );
        }
    }

    /**
     * Handles a BlueJ class state-change notification.
     * Publishes telemetry describing the class's compiled and error states.
     *
     * @param event the class event containing the current class state
     */
    @Override
    public void classStateChanged(ClassEvent event) {
        subject.publish(new TelemetryEvent(
            EventType.CLASS_STATE_CHANGED, 
            LocalDateTime.now(), 
            "Compiled: " + event.isClassCompiled()
                    + " | Has error: " + event.hasError(), 
            event.getBClass().getName()
        ));
    }

    /**
     * Handles a BlueJ class-removal notification.
     * Class removals are currently not tracked by this listener.
     *
     * @param event the class event for the removed class
     */
    @Override
    public void classRemoved(ClassEvent event) {
    }
}
