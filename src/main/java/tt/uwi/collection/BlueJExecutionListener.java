package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

public class BlueJExecutionListener
        implements InvocationFinishedListener {

    private final TelemetrySubject subject;

    public BlueJExecutionListener(TelemetrySubject subject) {
        this.subject = subject;
    }

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