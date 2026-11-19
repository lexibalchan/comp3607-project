package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Receives BlueJ invocation-completion events and publishes program-run
 * telemetry, with an additional runtime-exception event for exception exits.
 */

public class BlueJExecutionListener
        implements InvocationFinishedListener {

    /** Publishes execution telemetry to registered observers. */
    private final TelemetrySubject subject;

     /**
     * Constructs an execution listener.
     *
     * @param subject the subject used to publish telemetry
     */
    public BlueJExecutionListener(TelemetrySubject subject) {
        this.subject = subject;
    }

    /**
     * Handles a completed BlueJ invocation.
     * Publishes a program-run event containing the class, method, and outcome.
     * If the invocation ended with an exception, this method also publishes a
     * runtime-exception event at the same timestamp.
     *
     * @param event the invocation-completion event
     */
    @Override
    public void invocationFinished(InvocationFinishedEvent event) {
        LocalDateTime timestamp = LocalDateTime.now();

        String methodName = event.getMethodName();

        if (methodName != null && methodName.isBlank()) {
            methodName = null;
        }

        String displayedMethod =
                methodName == null ? "(not supplied)" : methodName;

        String outcome = event.getEventType().name();

        subject.publish(TelemetryEvent.execution(
                EventType.PROGRAM_RUN,
                timestamp,
                "Invocation finished | Method: " + displayedMethod
                        + " | Outcome: " + outcome,
                event.getClassName(),
                methodName,
                outcome
        ));

        if (event.getEventType()
                == InvocationFinishedEvent.EventType.EXCEPTION_EXIT) {

            subject.publish(TelemetryEvent.execution(
                    EventType.RUNTIME_EXCEPTION,
                    timestamp,
                    "Invocation ended with an exception"
                            + " | Method: " + displayedMethod,
                    event.getClassName(),
                    methodName,
                    outcome
            ));
        }
    }
}
