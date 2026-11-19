package tt.uwi.telemetry.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents one recorded telemetry event, including its category, timestamp,
 * display details, and optional source-location or execution information.
 */
public class TelemetryEvent {

    /** The category of the telemetry event. */
    private EventType type;

    /** The local time when the event occurred. */
    private LocalDateTime timestamp;

    /** Human-readable details describing the event. */
    private String description;

    /** The associated class name, or {@code null} when no class is known. */
    private String className;

    /** The optional source line number. */
    private Integer lineNumber;

    /** Absolute source-file paths associated with the event. */
    private List<String> sourceFiles = List.of();

    /** The optional invoked method name. */
    private String methodName;

    /** The optional program-execution outcome. */
    private String executionOutcome;

    /**
     * Constructs a basic telemetry event.
     *
     * @param type the event category
     * @param timestamp the local event timestamp
     * @param description the event description
     * @param className the associated class name, or {@code null}
     */
    public TelemetryEvent(EventType type, LocalDateTime timestamp,
                          String description, String className) {
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
        this.className = className;
    }

    /**
     * Constructs a telemetry event with an optional source line number.
     *
     * @param type the event category
     * @param timestamp the local event timestamp
     * @param description the event description
     * @param className the associated class name, or {@code null}
     * @param lineNumber the source line number, or {@code null}
     */
    public TelemetryEvent(EventType type, LocalDateTime timestamp,
                          String description, String className,
                        Integer lineNumber) {
        this(type, timestamp, description, className);
        this.lineNumber = lineNumber;
    }

    /**
     * Constructs a telemetry event with source line and file information.
     *
     * @param type the event category
     * @param timestamp the local event timestamp
     * @param description the event description
     * @param className the associated class name, or {@code null}
     * @param lineNumber the source line number, or {@code null}
     * @param sourceFiles absolute paths of source files associated with the event
     */
    public TelemetryEvent(EventType type, LocalDateTime timestamp,
                        String description, String className,
                        Integer lineNumber, List<String> sourceFiles) {
        this(type, timestamp, description, className, lineNumber);
        this.sourceFiles = List.copyOf(sourceFiles);
    }

    /**
     * Creates an execution-related telemetry event with method and outcome details.
     *
     * @param type the event category
     * @param timestamp the local event timestamp
     * @param description the event description
     * @param className the associated class name, or {@code null}
     * @param methodName the invoked method name, or {@code null}
     * @param executionOutcome the textual execution outcome
     * @return a new execution telemetry event
     */
    public static TelemetryEvent execution(EventType type, LocalDateTime timestamp,
                                            String description, String className, 
                                            String methodName, String executionOutcome) {
        TelemetryEvent event = new TelemetryEvent(type, timestamp, description, className);

        event.methodName = methodName;
        event.executionOutcome = executionOutcome;

        return event;
    }

    /** @return the event category */
    public EventType getType() {
        return type;
    }

    /** @return the local event timestamp */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** @return the human-readable event description */
    public String getDescription() {
        return description;
    }

    /** @return the associated class name, or {@code null} */
    public String getClassName() {
        return className;
    }

    /** @return the source line number, or {@code null} */
    public Integer getLineNumber() {
        return lineNumber;
    }

    /** @return an immutable copy of the associated source-file paths */
    public List<String> getSourceFiles() {
        return sourceFiles;
    }
    
    /** @return the invoked method name, or {@code null} */
    public String getMethodName() {
        return methodName;
    }

    /** @return the execution outcome, or {@code null} */
    public String getExecutionOutcome() {
        return executionOutcome;
    }
}
