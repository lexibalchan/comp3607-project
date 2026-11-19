package tt.uwi.collection;

import java.util.ArrayList;
import java.util.List;
import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Maintains telemetry observers and publishes events to each registered
 * observer in registration order.
 */
public class TelemetrySubject {

    /** Observers that receive published telemetry events. */
    private List<TelemetryObserver> observers = new ArrayList<>();

    /**
     * Registers an observer to receive subsequent telemetry events.
     *
     * @param observer the observer to add
     */
    public void addObserver(TelemetryObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes an observer previously registered with this subject.
     * Removing an observer that is not registered has no effect.
     *
     * @param observer the observer to remove
     */
    public void removeObserver(TelemetryObserver observer) {
        observers.remove(observer);
    }

    /**
     * Publishes a telemetry event to every currently registered observer.
     *
     * @param event the telemetry event to distribute
     */
    public void publish(TelemetryEvent event) {
        for (TelemetryObserver observer : observers) {
            observer.onTelemetryEvent(event);
        }
    }
}
