package tt.uwi.telemetry.model;

import java.time.LocalDateTime;
import java.util.List;

public class TelemetryEvent {

    private EventType type;
    private LocalDateTime timestamp;
    private String description;
    private String className;
    private Integer lineNumber;
    private List<String> sourceFiles = List.of();
    private String methodName;
    private String executionOutcome;

    public TelemetryEvent(EventType type, LocalDateTime timestamp,
                          String description, String className) {
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
        this.className = className;
    }

    public TelemetryEvent(EventType type, LocalDateTime timestamp,
                          String description, String className,
                        Integer lineNumber) {
        this(type, timestamp, description, className);
        this.lineNumber = lineNumber;
    }

    public TelemetryEvent(EventType type, LocalDateTime timestamp,
                        String description, String className,
                        Integer lineNumber, List<String> sourceFiles) {
        this(type, timestamp, description, className, lineNumber);
        this.sourceFiles = List.copyOf(sourceFiles);
    }

    public static TelemetryEvent execution(EventType type, LocalDateTime timestamp,
                                            String description, String className, 
                                            String methodName, String executionOutcome) {
        TelemetryEvent event = new TelemetryEvent(type, timestamp, description, className);

        event.methodName = methodName;
        event.executionOutcome = executionOutcome;

        return event;
    }

    public EventType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public String getClassName() {
        return className;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }
    
    public String getMethodName() {
        return methodName;
    }

    public String getExecutionOutcome() {
        return executionOutcome;
    }
}