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

public class SessionAnalytics {

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