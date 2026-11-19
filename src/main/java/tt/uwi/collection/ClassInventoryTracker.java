package tt.uwi.collection;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Tracks class names by package key and publishes telemetry when class
 * creation, deletion, or renaming is detected.
 */
public class ClassInventoryTracker {

    /** Publishes detected class inventory changes. */
    private final TelemetrySubject subject;

    /** Stores the latest observed class-name set for each package key. */
    private final Map<String, Set<String>> previousInventories =
            new HashMap<>();

    /**
     * Constructs a class inventory tracker.
     *
     * @param subject the subject used to publish inventory changes
     */
    public ClassInventoryTracker(TelemetrySubject subject) {
        this.subject = subject;
    }

    /**
     * Compares a package's current class names with its previously observed
     * inventory. The first observation establishes a baseline without
     * publishing creation events.
     *
     * @param packageKey the key identifying the package
     * @param classNames the class names currently in the package
     */
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

    /**
     * Publishes a class creation or deletion event for a package.
     *
     * @param type the class inventory event type
     * @param packageKey the key identifying the package
     * @param className the affected class name
     */
    private void publish(
            EventType type, String packageKey, String className) {

        subject.publish(new TelemetryEvent(
                type,
                LocalDateTime.now(),
                "Detected by class-list comparison in: " + packageKey,
                className
        ));
    }

    /**
     * Records a class rename and publishes a rename event.
     * If the old name is in the package baseline, the baseline is updated to
     * the new name so no separate creation or deletion event is generated.
     *
     * @param packageKey the key identifying the package
     * @param oldName the former class name
     * @param newName the current class name
     */
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

    /**
     * Removes a package's stored class baseline.
     * The next observation will establish a fresh baseline.
     *
     * @param packageKey the key identifying the package to forget
     */
    public void forget(String packageKey) {
        previousInventories.remove(packageKey);
    }
}
