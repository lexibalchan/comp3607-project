package tt.uwi.analytics;

import java.time.Duration;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

/**
 * Provides estimates of active and idle time within a telemetry session.
 * Activity is estimated from editor events, with each event representing
 * activity for a configurable period following the event.
 */

public class ActivityAnalytics {

    /**
     * Analyses editor activity in a telemetry session.
     *
     * <p>Each editor event contributes activity up to
     * {@code idleThresholdSeconds} after that event. Overlapping activity
     * periods are counted only once. Editor events outside the session's
     * start and end times are ignored.</p>
     *
     * @param session the telemetry session to analyse
     * @param idleThresholdSeconds the number of seconds following an editor
     *                             event that are considered active
     * @return a summary containing the editor-event count and estimated active
     *         and idle durations
     * @throws IllegalArgumentException if the idle threshold is not positive
     *                                  or the session ends before it starts
     * @throws IllegalStateException if the session does not have an end time
     */

    public ActivitySummary analyse(
            TelemetrySession session, int idleThresholdSeconds) {

        if (idleThresholdSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Idle threshold must be positive."
            );
        }

        if (session.getEndTime() == null) {
            throw new IllegalStateException(
                    "Session must have an end time."
            );
        }

        double sessionSeconds = Duration.between(
                session.getStartTime(),
                session.getEndTime()
        ).toMillis() / 1000.0;

        if (sessionSeconds < 0) {
            throw new IllegalArgumentException(
                    "Session end cannot precede its start."
            );
        }

        var editTimes = session.getEvents().stream()
                .filter(event ->
                        event.getType() == EventType.EDITOR_ACTIVITY)
                .map(TelemetryEvent::getTimestamp)
                .filter(time ->
                        !time.isBefore(session.getStartTime())
                        && !time.isAfter(session.getEndTime()))
                .sorted()
                .toList();

        double activeSeconds = 0.0;
        double coveredUntil = 0.0;

        for (var time : editTimes) {
            double start = Duration.between(
                    session.getStartTime(), time
            ).toMillis() / 1000.0;

            double end = Math.min(
                    start + idleThresholdSeconds,
                    sessionSeconds
            );

            activeSeconds += Math.max(
                    0.0,
                    end - Math.max(start, coveredUntil)
            );

            coveredUntil = Math.max(coveredUntil, end);
        }

        return new ActivitySummary(
                editTimes.size(),
                activeSeconds,
                Math.max(0.0, sessionSeconds - activeSeconds)
        );
    }
}
