package tt.uwi.collection;

import java.util.ArrayList;
import java.util.List;
import tt.uwi.telemetry.model.TelemetryEvent;

public class TelemetrySubject {

    private List<TelemetryObserver> observers = new ArrayList<>();

    public void addObserver(TelemetryObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TelemetryObserver observer) {
        observers.remove(observer);
    }

    public void publish(TelemetryEvent event) {
        for (TelemetryObserver observer : observers) {
            observer.onTelemetryEvent(event);
        }
    }
}