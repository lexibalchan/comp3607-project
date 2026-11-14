package tt.uwi.collection;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;

public class ClassInventoryTrackerTest {

    @Test
    void firstObservationDoesNotReportExistingClassesAsNew() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        ClassInventoryTracker tracker =
                new ClassInventoryTracker(subject);

        tracker.observe("test-project/default", Set.of("Student"));

        assertTrue(session.getEvents().isEmpty());
    }

    @Test
    void detectsAddedAndRemovedClassesWithoutRepeatingEvents() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        ClassInventoryTracker tracker =
                new ClassInventoryTracker(subject);

        String packageKey = "test-project/default";

        tracker.observe(packageKey, Set.of("Student"));
        tracker.observe(packageKey, Set.of("Student", "Course"));
        tracker.observe(packageKey, Set.of("Course"));
        tracker.observe(packageKey, Set.of("Course"));

        assertEquals(2, session.getEvents().size());

        var created = session.getEvents().get(0);
        assertEquals(EventType.CLASS_CREATED, created.getType());
        assertEquals("Course", created.getClassName());

        var deleted = session.getEvents().get(1);
        assertEquals(EventType.CLASS_DELETED, deleted.getType());
        assertEquals("Student", deleted.getClassName());
    }

    @Test
    void reopeningPackageEstablishesAFreshBaseline() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        ClassInventoryTracker tracker =
                new ClassInventoryTracker(subject);

        String packageKey = "test-project/default";

        tracker.observe(packageKey, Set.of("Student"));
        tracker.forget(packageKey);

        tracker.observe(packageKey, Set.of("Course"));

        assertTrue(session.getEvents().isEmpty());

        tracker.observe(packageKey, Set.of("Course", "Teacher"));

        assertEquals(1, session.getEvents().size());
        assertEquals(
                EventType.CLASS_CREATED,
                session.getEvents().get(0).getType()
        );
        assertEquals(
                "Teacher",
                session.getEvents().get(0).getClassName()
        );
    }

    @Test
    void renameUpdatesBaselineWithoutExtraCreationOrDeletion() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        ClassInventoryTracker tracker =
                new ClassInventoryTracker(subject);

        String packageKey = "test-project/default";

        tracker.observe(packageKey, Set.of("Student", "Course"));

        tracker.recordRename(packageKey, "Student", "Learner");

        tracker.observe(packageKey, Set.of("Learner", "Course"));

        assertEquals(1, session.getEvents().size());

        var renamed = session.getEvents().get(0);

        assertEquals(EventType.CLASS_RENAMED, renamed.getType());
        assertEquals("Learner", renamed.getClassName());
        assertTrue(
                renamed.getDescription()
                        .contains("Renamed from Student to Learner")
        );
    }
}