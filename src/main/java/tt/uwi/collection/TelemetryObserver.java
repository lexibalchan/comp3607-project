package tt.uwi.collection;

import tt.uwi.telemetry.model.TelemetryEvent;

public interface TelemetryObserver {

    void onTelemetryEvent(TelemetryEvent event);
}
