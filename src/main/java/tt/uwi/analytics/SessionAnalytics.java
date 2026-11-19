package tt.uwi.analytics;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

import java.util.Map;
import java.util.TreeMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates aggregate and event-based analytics for telemetry sessions.
 */
public class SessionAnalytics {
    
    /**
     * Creates a summary of the session's overall event and activity counts.
     * Compilation attempts include successful and failed compilation events.
     *
     * @param session the telemetry session to analyse
     * @return an analytics summary for the session
     */
    public AnalyticsSummary analyse(TelemetrySession session) {

        int attempts = 0;
        int failures = 0;
        int runs = 0;
        int classesCreated = 0;

        for (TelemetryEvent event : session.getEvents()) {

            if (event.getType() == EventType.COMPILE_SUCCESS ||
                    event.getType() == EventType.COMPILE_FAILURE) {
                attempts++;
            }

            if (event.getType() == EventType.COMPILE_FAILURE) {
                failures++;
            }

            if (event.getType() == EventType.PROGRAM_RUN) {
                runs++;
            }

            if (event.getType() == EventType.CLASS_CREATED) {
                classesCreated++;
            }
        }

        return new AnalyticsSummary(
                session.getEvents().size(),
                attempts,
                failures,
                runs,
                classesCreated
        );
    }

    /**
     * Counts compile errors grouped by their compiler-message description.
     * Events with a null or blank description are grouped under
     * {@code "Unkown compiler error"}.
     *
     * @param session the telemetry session containing compile-error events
     * @return a map of compiler messages to their occurrence counts
     */
    public Map<String, Integer> countErrorsByMessage(TelemetrySession session) {
        Map<String, Integer> counts = new TreeMap<>();

        for (TelemetryEvent event : session.getEvents()) {
            if (event.getType() == EventType.COMPILE_ERROR) {
                String message = event.getDescription();

                if (message == null || message.isBlank()) {
                    message = "Unkown compiler error";
                }

                counts.merge(message, 1, Integer::sum);
            }
        }

        return counts;
    }

    /**
     * Calculates the time between consecutive compilation-start events.
     * The interval before the first compilation-start event is not included.
     *
     * @param session the telemetry session containing compile-start events
     * @return the compile intervals in seconds, in event order
     */
    public List<Double> calculateCompileIntervalsSeconds(TelemetrySession session) {

        List<Double> intervals = new ArrayList<>();
        LocalDateTime previousStart = null;

        for (TelemetryEvent event : session.getEvents()) {
            if (event.getType() != EventType.COMPILE_STARTED) {
                continue;
            }

            LocalDateTime currentStart = event.getTimestamp();

            if (previousStart != null) {
                double seconds = Duration.between(
                        previousStart,
                        currentStart
                ).toMillis() / 1000.0;

                intervals.add(seconds);
            }

            previousStart = currentStart;
        }

        return intervals;
    }
}
