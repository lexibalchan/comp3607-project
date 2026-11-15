package tt.uwi.collection;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

public class MethodInventoryTracker {

    private record ClassKey(String packageDirectory, String className) {}

    private final TelemetrySubject subject;
    private final Map<ClassKey, Set<String>> previousMethods =
            new HashMap<>();

    public MethodInventoryTracker(TelemetrySubject subject) {
        this.subject = subject;
    }

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

    public void forgetClass(String packageDirectory, String className) {
        previousMethods.remove(
                new ClassKey(packageDirectory, className)
        );
    }

    public void forgetPackage(String packageDirectory) {
        previousMethods.keySet().removeIf(
                key -> key.packageDirectory().equals(packageDirectory)
        );
    }

    public void retainClasses(String packageDirectory, Set<String> currentClassNames) {
        previousMethods.keySet().removeIf(
            key -> key.packageDirectory().equals(packageDirectory)
                    && !currentClassNames.contains(key.className())
        );
    }
}