package tt.uwi.collection;

import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Observer for telemetry events published by a telemetry subject.
 * Implementations handle each event without being responsible for publishing it.
 */
public interface TelemetryObserver {

    /**
     * Receives a telemetry event when it is published.
     *
     * @param event the telemetry event that occurred
     */
    void onTelemetryEvent(TelemetryEvent event);
}
