package tt.uwi.analytics;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.List;

public class SessionAnalyticsTest {
    
    @Test 
    void countsSuccessfulAndFailedCompilations() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 9, 10, 0);

        TelemetrySession session = new TelemetrySession("TEST-STUDENT", time);

        session.addEvent(new TelemetryEvent(
            EventType.COMPILE_SUCCESS,
            time,
            "Compilation succeeded",
            "Student"
        ));

        session.addEvent(new TelemetryEvent(
            EventType.COMPILE_FAILURE,
            time,
            "Compilation failed",
            "Student"
        ));

        AnalyticsSummary summary = new SessionAnalytics().analyse(session);

        assertEquals(2, summary.getTotalEvents());
        assertEquals(2, summary.getCompilationAttempts());
        assertEquals(1, summary.getCompilationFailures());
    }

    @Test
    void errorDiagnosticsDoNotCountAsExtraCompilationAttempts() {
        LocalDateTime time =
                LocalDateTime.of(2026, 11, 10, 11, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", time);
        
        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED, time, 
                "Compilation started", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR,
                time,
                "';' expected",
                "Student.java",
                11
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR,
                time,
                "cannot find symbol",
                "Student.java",
                12
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_FAILURE,
                time,
                "Compilation failed",
                null
        ));

        AnalyticsSummary summary =
                new SessionAnalytics().analyse(session);

        assertEquals(4, summary.getTotalEvents());
        assertEquals(1, summary.getCompilationAttempts());
        assertEquals(1, summary.getCompilationFailures());
    }

    @Test
    void groupsCompilerErrorsByMessage() {
        LocalDateTime time =
                LocalDateTime.of(2026, 11, 10, 11, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", time);

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR, time,
                "';' expected", "Student.java", 11
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR, time,
                "';' expected", "Student.java", 15
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR, time,
                "cannot find symbol", "Student.java", 20
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_FAILURE, time,
                "Compilation failed", null
        ));

        Map<String, Integer> counts =
                new SessionAnalytics().countErrorsByMessage(session);

        assertEquals(
                Map.of(
                        "';' expected", 2,
                        "cannot find symbol", 1
                ),
                counts
        );
    }

    @Test
    void calculatesIntervalsBetweenCompilationStarts() {
        LocalDateTime time =
                LocalDateTime.of(2026, 11, 10, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", time);

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED, time,
                "Compilation started", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_SUCCESS, time.plusSeconds(1),
                "Compilation succeeded", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED, time.plusSeconds(12),
                "Compilation started", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED,
                time.plusSeconds(30).plusNanos(500_000_000),
                "Compilation started", null
        ));

        List<Double> intervals =
                new SessionAnalytics()
                        .calculateCompileIntervalsSeconds(session);

        assertEquals(2, intervals.size());
        assertEquals(12.0, intervals.get(0), 0.001);
        assertEquals(18.5, intervals.get(1), 0.001);
    }

    @Test
    void returnsNoIntervalsUntilThereAreTwoStarts() {
        LocalDateTime time =
                LocalDateTime.of(2026, 11, 10, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", time);

        SessionAnalytics analytics = new SessionAnalytics();

        assertEquals(
                List.of(),
                analytics.calculateCompileIntervalsSeconds(session)
        );

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED, time,
                "Compilation started", null
        ));

        assertEquals(
                List.of(),
                analytics.calculateCompileIntervalsSeconds(session)
        );
    }
    
}
