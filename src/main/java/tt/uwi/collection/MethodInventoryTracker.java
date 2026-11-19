package tt.uwi.collection;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Tracks declared method signatures for each class in a package directory and
 * publishes telemetry when method additions or removals are detected.
 */
public class MethodInventoryTracker {

    /** Identifies a class within a package directory. */
    private record ClassKey(String packageDirectory, String className) {}

    /** Publishes detected method inventory changes. */
    private final TelemetrySubject subject;

    /** Stores the latest observed method-signature set for each class. */
    private final Map<ClassKey, Set<String>> previousMethods =
            new HashMap<>();

    /**
     * Constructs a method inventory tracker.
     *
     * @param subject the subject used to publish method changes
     */
    public MethodInventoryTracker(TelemetrySubject subject) {
        this.subject = subject;
    }

    /**
     * Compares a class's current method signatures with its previously stored
     * inventory. The first observation establishes a baseline without
     * publishing method-change events.
     *
     * @param packageDirectory the absolute package-directory key
     * @param className the class whose methods are being observed
     * @param currentMethods the class's current method signatures
     */
    public void observe(
            String packageDirectory,
            String className,
            Set<String> currentMethods) {

        ClassKey key = new ClassKey(packageDirectory, className);

        Set<String> previous = previousMethods.put(
                key, Set.copyOf(currentMethods)
        );

        if (previous == null) {
            return;
        }

        Set<String> added = new TreeSet<>(currentMethods);
        added.removeAll(previous);

        Set<String> removed = new TreeSet<>(previous);
        removed.removeAll(currentMethods);

        LocalDateTime detectedAt = LocalDateTime.now();

        for (String signature : added) {
            subject.publish(new TelemetryEvent(
                    EventType.METHOD_ADDED,
                    detectedAt,
                    "Detected method addition: " + signature
                            + " | Package directory: " + packageDirectory,
                    className
            ));
        }

        for (String signature : removed) {
            subject.publish(new TelemetryEvent(
                    EventType.METHOD_REMOVED,
                    detectedAt,
                    "Detected method removal: " + signature
                            + " | Package directory: " + packageDirectory,
                    className
            ));
        }
    }

    /**
     * Removes stored method inventory for one class.
     * The next observation of the class establishes a fresh baseline.
     *
     * @param packageDirectory the class package directory
     * @param className the class to forget
     */
    public void forgetClass(String packageDirectory, String className) {
        previousMethods.remove(
                new ClassKey(packageDirectory, className)
        );
    }

    /**
     * Removes stored method inventory for every class in a package directory.
     *
     * @param packageDirectory the package directory to forget
     */
    public void forgetPackage(String packageDirectory) {
        previousMethods.keySet().removeIf(
                key -> key.packageDirectory().equals(packageDirectory)
        );
    }

    /**
     * Removes stored method inventory for classes no longer present in a
     * package. Stored inventory for the remaining classes is preserved.
     *
     * @param packageDirectory the package directory being retained
     * @param currentClassNames the class names currently present in the package
     */
    public void retainClasses(String packageDirectory, Set<String> currentClassNames) {
        previousMethods.keySet().removeIf(
            key -> key.packageDirectory().equals(packageDirectory)
                    && !currentClassNames.contains(key.className())
        );
    }
}
