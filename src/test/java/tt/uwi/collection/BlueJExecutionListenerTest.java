package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.event.InvocationFinishedEvent;
import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlueJExecutionListenerTest {

    @Test
    void normalCompletionRecordsOneRunWithMethodDetails() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        InvocationFinishedEvent notification =
                mock(InvocationFinishedEvent.class);

        when(notification.getClassName()).thenReturn("Learner");
        when(notification.getMethodName()).thenReturn("successfulRun");
        when(notification.getEventType()).thenReturn(
                InvocationFinishedEvent.EventType.NORMAL_EXIT
        );

        new BlueJExecutionListener(subject)
                .invocationFinished(notification);

        assertEquals(1, session.getEvents().size());
        assertEquals(1L, session.getTotalRuns());

        var recorded = session.getEvents().get(0);

        assertEquals(EventType.PROGRAM_RUN, recorded.getType());
        assertEquals("Learner", recorded.getClassName());
        assertEquals("successfulRun", recorded.getMethodName());
        assertEquals("NORMAL_EXIT", recorded.getExecutionOutcome());
    }

    @Test
    void exceptionalCompletionRecordsDiagnosticWithoutExtraRun() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        InvocationFinishedEvent notification =
                mock(InvocationFinishedEvent.class);

        when(notification.getClassName()).thenReturn("Learner");
        when(notification.getMethodName()).thenReturn(null);
        when(notification.getEventType()).thenReturn(
                InvocationFinishedEvent.EventType.EXCEPTION_EXIT
        );

        new BlueJExecutionListener(subject)
                .invocationFinished(notification);

        assertEquals(2, session.getEvents().size());
        assertEquals(1L, session.getTotalRuns());

        var run = session.getEvents().get(0);
        var diagnostic = session.getEvents().get(1);

        assertEquals(EventType.PROGRAM_RUN, run.getType());
        assertEquals(EventType.RUNTIME_EXCEPTION, diagnostic.getType());

        assertNull(run.getMethodName());
        assertNull(diagnostic.getMethodName());

        assertEquals("EXCEPTION_EXIT", run.getExecutionOutcome());
        assertEquals("EXCEPTION_EXIT", diagnostic.getExecutionOutcome());
        assertEquals("Learner", diagnostic.getClassName());
        assertEquals(run.getTimestamp(), diagnostic.getTimestamp());
    }
}