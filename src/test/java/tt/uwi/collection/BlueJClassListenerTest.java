package tt.uwi.collection;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Set;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.event.ClassEvent;

import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlueJClassListenerTest {

    @Test
    void recordsCompiledAndErrorState() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        BClass bluejClass = mock(BClass.class);
        ClassEvent notification = mock(ClassEvent.class);

        when(notification.getBClass()).thenReturn(bluejClass);
        when(bluejClass.getName()).thenReturn("Learner");
        when(notification.isClassCompiled()).thenReturn(false);
        when(notification.hasError()).thenReturn(true);

        BlueJClassListener listener = new BlueJClassListener(
                new ClassInventoryTracker(subject),
                subject
        );

        listener.classStateChanged(notification);

        assertEquals(1, session.getEvents().size());

        var recorded = session.getEvents().get(0);

        assertEquals(EventType.CLASS_STATE_CHANGED, recorded.getType());
        assertEquals("Learner", recorded.getClassName());
        assertEquals(
                "Compiled: false | Has error: true",
                recorded.getDescription()
        );
        assertEquals(0L, session.getTotalCompilations());
    }

    @Test
    void renameNotificationUpdatesInventoryWithoutDuplicateEvents()
            throws Exception {

        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        ClassInventoryTracker tracker =
                new ClassInventoryTracker(subject);

        File directory = new File("test-project");
        String packageKey = directory.getAbsolutePath();

        tracker.observe(packageKey, Set.of("Student"));

        BClass bluejClass = mock(BClass.class);
        BPackage bluejPackage = mock(BPackage.class);
        ClassEvent notification = mock(ClassEvent.class);

        when(notification.getPackage()).thenReturn(bluejPackage);
        when(bluejPackage.getDir()).thenReturn(directory);
        when(notification.getBClass()).thenReturn(bluejClass);
        when(notification.getOldName()).thenReturn("Student");
        when(bluejClass.getName()).thenReturn("Learner");

        new BlueJClassListener(tracker, subject)
                .classNameChanged(notification);

        tracker.observe(packageKey, Set.of("Learner"));

        assertEquals(1, session.getEvents().size());

        var recorded = session.getEvents().get(0);

        assertEquals(EventType.CLASS_RENAMED, recorded.getType());
        assertEquals("Learner", recorded.getClassName());
        assertTrue(recorded.getDescription()
                .contains("Renamed from Student to Learner"));
    }
}