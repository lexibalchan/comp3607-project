package tt.uwi.analytics;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActivityAnalyticsTest {

    @Test
    void overlappingActivityPeriodsAreCountedOnlyOnce() {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        // Deliberately supplied out of order to check sorting.
        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY, start.plusSeconds(20),
                "Editor document changed", "Learner"
        ));

        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY, start.plusSeconds(10),
                "Editor document changed", "Learner"
        ));

        session.setEndTime(start.plusSeconds(100));

        ActivitySummary summary =
                new ActivityAnalytics().analyse(session, 30);

        assertEquals(2L, summary.editEvents());
        assertEquals(40.0, summary.estimatedActiveSeconds(), 0.001);
        assertEquals(60.0, summary.estimatedIdleSeconds(), 0.001);
    }

    @Test
    void activityPeriodStopsAtSessionEnd() {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY, start.plusSeconds(95),
                "Editor document changed", "Learner"
        ));

        session.setEndTime(start.plusSeconds(100));

        ActivitySummary summary =
                new ActivityAnalytics().analyse(session, 30);

        assertEquals(1L, summary.editEvents());
        assertEquals(5.0, summary.estimatedActiveSeconds(), 0.001);
        assertEquals(95.0, summary.estimatedIdleSeconds(), 0.001);
    }

    @Test
    void sessionWithoutEditsHasNoEstimatedActiveTime() {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_SUCCESS, start.plusSeconds(10),
                "Compilation succeeded", "Learner"
        ));

        session.addEvent(new TelemetryEvent(
                EventType.PROGRAM_RUN, start.plusSeconds(20),
                "Invocation finished", "Learner"
        ));

        session.setEndTime(start.plusSeconds(60));

        ActivitySummary summary =
                new ActivityAnalytics().analyse(session, 30);

        assertEquals(0L, summary.editEvents());
        assertEquals(0.0, summary.estimatedActiveSeconds(), 0.001);
        assertEquals(60.0, summary.estimatedIdleSeconds(), 0.001);
    }
}