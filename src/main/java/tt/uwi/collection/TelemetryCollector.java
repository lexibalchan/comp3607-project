package tt.uwi.collection;

import java.util.ArrayList;
import java.util.List;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

public class TelemetryCollector implements TelemetryObserver {

    private TelemetrySession session;

    public TelemetryCollector(TelemetrySession session) {
        this.session = session;
    }

    @Override
    public void onTelemetryEvent(TelemetryEvent event) {
        session.addEvent(event);
    }

    public List<TelemetryEvent> getEvents() {
        return new ArrayList<>(session.getEvents());
    }
}