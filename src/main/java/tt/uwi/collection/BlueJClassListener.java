package tt.uwi.collection;

import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;

import java.time.LocalDateTime;
import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

public class BlueJClassListener implements ClassListener {

    private final ClassInventoryTracker tracker;
    private final TelemetrySubject subject;

    public BlueJClassListener(ClassInventoryTracker tracker, TelemetrySubject subject) {
        this.tracker = tracker;
        this.subject = subject;
    }

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

    @Override
    public void classRemoved(ClassEvent event) {
    }
}