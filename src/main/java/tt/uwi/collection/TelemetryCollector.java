package tt.uwi.collection;

import java.util.ArrayList;
import java.util.List;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

/**
 * Collects published telemetry events by adding them to a telemetry session.
 */
public class TelemetryCollector implements TelemetryObserver {

    /** The telemetry session receiving collected events. */
    private TelemetrySession session;

    /**
     * Constructs a collector for a telemetry session.
     *
     * @param session the session that will receive published events
     */
    public TelemetryCollector(TelemetrySession session) {
        this.session = session;
    }

    /**
     * Adds a published telemetry event to the configured session.
     *
     * @param event the telemetry event to collect
     */
    @Override
    public void onTelemetryEvent(TelemetryEvent event) {
        session.addEvent(event);
    }

    /**
     * Returns a copy of the events currently stored in the session.
     *
     * @return a new list containing the session's telemetry events
     */
    public List<TelemetryEvent> getEvents() {
        return new ArrayList<>(session.getEvents());
    }
}
