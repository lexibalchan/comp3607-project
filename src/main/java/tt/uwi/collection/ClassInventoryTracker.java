package tt.uwi.collection;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

public class ClassInventoryTracker {

    private final TelemetrySubject subject;
    private final Map<String, Set<String>> previousInventories =
            new HashMap<>();

    public ClassInventoryTracker(TelemetrySubject subject) {
        this.subject = subject;
    }

    public void observe(String packageKey, Set<String> classNames) {
        Set<String> current = Set.copyOf(classNames);
        Set<String> previous = previousInventories.put(
                packageKey, current
        );

        if (previous == null) {
            return;
        }

        Set<String> added = new TreeSet<>(current);
        added.removeAll(previous);

        Set<String> removed = new TreeSet<>(previous);
        removed.removeAll(current);

        for (String className : added) {
            publish(EventType.CLASS_CREATED, packageKey, className);
        }

        for (String className : removed) {
            publish(EventType.CLASS_DELETED, packageKey, className);
        }
    }

    private void publish(
            EventType type, String packageKey, String className) {

        subject.publish(new TelemetryEvent(
                type,
                LocalDateTime.now(),
                "Detected by class-list comparison in: " + packageKey,
                className
        ));
    }

    public void recordRename(String packageKey, String oldName, String newName) {
        Set<String> previous = previousInventories.get(packageKey);

        if (previous != null && previous.contains(oldName)) {
            Set<String> updated = new HashSet<>(previous);
            updated.remove(oldName);
            updated.add(newName);

            previousInventories.put(
                packageKey,
                Set.copyOf(updated)
            );
        }

        subject.publish(new TelemetryEvent(
            EventType.CLASS_RENAMED, 
            LocalDateTime.now(), 
            "Renamed from " + oldName + " to " + newName 
            + " in: " + packageKey, newName
        ));
    }

    public void forget(String packageKey) {
        previousInventories.remove(packageKey);
    }
}