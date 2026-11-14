package tt.uwi.telemetry.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TelemetrySessionTest {

    @Test
    void calculatesDurationIncludingFractionalSeconds() {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 9, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.setEndTime(
                start.plusSeconds(9).plusNanos(456_000_000)
        );

        assertEquals(
                9.456,
                session.getSessionDurationSeconds(),
                0.001
        );
    }

    @Test
    void durationIsUnknownUntilSessionEnds() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 9, 10, 0)
        );

        assertNull(session.getSessionDurationSeconds());
    }

    @Test
    void compilationTotalsIgnoreErrorDiagnostics() {
        LocalDateTime time =
                LocalDateTime.of(2026, 11, 10, 12, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", time);

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_SUCCESS, time,
                "Compilation succeeded", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED, time, 
                "Compilation started", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR, time,
                "';' expected", "Student.java", 11
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR, time,
                "cannot find symbol", "Student.java", 12
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_FAILURE, time,
                "Compilation failed", null
        ));

        assertEquals(1L, session.getSuccessfulCompilations());
        assertEquals(1L, session.getFailedCompilations());
        assertEquals(2L, session.getTotalCompilations());
    }

    @Test
    void runtimeExceptionDoesNotCountAsAnAdditionalRun() {
        LocalDateTime time =
                LocalDateTime.of(2026, 11, 13, 9, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", time);

        session.addEvent(new TelemetryEvent(
                EventType.PROGRAM_RUN, time,
                "Invocation finished | Outcome: NORMAL_EXIT",
                "Learner"
        ));

        session.addEvent(new TelemetryEvent(
                EventType.PROGRAM_RUN, time.plusSeconds(5),
                "Invocation finished | Outcome: EXCEPTION_EXIT",
                "Learner"
        ));

        session.addEvent(new TelemetryEvent(
                EventType.RUNTIME_EXCEPTION, time.plusSeconds(5),
                "Invocation ended with an exception",
                "Learner"
        ));

        assertEquals(3, session.getEvents().size());
        assertEquals(2L, session.getTotalRuns());
    }

    @Test
    void snapshotKeepsItsEventsWhileLiveSessionContinues() {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_SUCCESS, start.plusSeconds(5),
                "Compilation succeeded", "Learner"
        ));

        TelemetrySession snapshot =
                session.snapshot(start.plusSeconds(10));

        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY, start.plusSeconds(15),
                "Editor document changed", "Learner"
        ));

        assertEquals(1, snapshot.getEvents().size());
        assertEquals(2, session.getEvents().size());

        assertEquals(start, snapshot.getStartTime());
        assertEquals("TEST-STUDENT", snapshot.getStudentId());
        assertEquals(start.plusSeconds(10), snapshot.getEndTime());
        assertEquals(10.0, snapshot.getSessionDurationSeconds(), 0.001);

        assertNull(session.getEndTime());
    }
}