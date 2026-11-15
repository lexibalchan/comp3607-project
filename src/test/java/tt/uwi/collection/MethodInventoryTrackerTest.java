package tt.uwi.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

import static org.junit.jupiter.api.Assertions.*;

class MethodInventoryTrackerTest {

    @Test
    void detectsChangedOverloadsWithoutRepeatingEvents() {
        List<TelemetryEvent> events = new ArrayList<>();
        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(events::add);

        MethodInventoryTracker tracker =
                new MethodInventoryTracker(subject);

        tracker.observe(
                "example/package", "Student",
                Set.of("calculate(int)")
        );

        assertTrue(events.isEmpty());

        tracker.observe(
                "example/package", "Student",
                Set.of("calculate(String)")
        );

        assertEquals(2, events.size());

        assertEquals(EventType.METHOD_ADDED, events.get(0).getType());
        assertTrue(events.get(0).getDescription()
                .contains("calculate(String)"));
        assertEquals("Student", events.get(0).getClassName());

        assertEquals(EventType.METHOD_REMOVED, events.get(1).getType());
        assertTrue(events.get(1).getDescription()
                .contains("calculate(int)"));

        tracker.observe(
                "example/package", "Student",
                Set.of("calculate(String)")
        );

        assertEquals(2, events.size());
    }

    @Test
    void keepsProjectsSeparateAndResetsForgottenPackage() {
        List<TelemetryEvent> events = new ArrayList<>();
        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(events::add);

        MethodInventoryTracker tracker =
                new MethodInventoryTracker(subject);

        tracker.observe("project-one", "Student", Set.of("first()"));
        tracker.observe("project-two", "Student", Set.of("second()"));

        assertTrue(events.isEmpty());

        tracker.forgetPackage("project-one");
        tracker.observe("project-one", "Student", Set.of("replacement()"));

        assertTrue(events.isEmpty());

        tracker.observe("project-two", "Student", Set.of());

        assertEquals(1, events.size());
        assertEquals(EventType.METHOD_REMOVED, events.get(0).getType());
        assertTrue(events.get(0).getDescription().contains("second()"));
    }

    @Test
    void clearsDeletedClassWithoutErasingRemainingClassHistory() {
        List<TelemetryEvent> events = new ArrayList<>();
        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(events::add);

        MethodInventoryTracker tracker =
                new MethodInventoryTracker(subject);

        tracker.observe("project-one", "Student", Set.of("oldMethod()"));
        tracker.observe("project-one", "Course", Set.of("enrol()"));

        // Student has been deleted; Course remains.
        tracker.retainClasses("project-one", Set.of("Course"));

        // A new Student class should establish a fresh baseline.
        tracker.observe("project-one", "Student", Set.of("newMethod()"));

        assertTrue(events.isEmpty());

        // Course should still have its previous method history.
        tracker.observe("project-one", "Course", Set.of());

        assertEquals(1, events.size());
        assertEquals(EventType.METHOD_REMOVED, events.get(0).getType());
        assertEquals("Course", events.get(0).getClassName());
        assertTrue(events.get(0).getDescription().contains("enrol()"));
    }
}