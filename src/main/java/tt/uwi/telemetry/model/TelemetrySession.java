package tt.uwi.telemetry.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

/**
 * Stores a student's telemetry session, including its time range, registered
 * projects, and collected telemetry events.
 */
public class TelemetrySession {

    /** Identifier of the student associated with the session. */
    private String studentId;

    /** Local time at which the session started. */
    private LocalDateTime startTime;

    /** Local time at which the session ended, or {@code null} while live. */
    private LocalDateTime endTime;

    /** Telemetry events collected in this session. */
    private List<TelemetryEvent> events;

    /** Unique project-directory paths associated with the session. */
    private final List<String> projectDirectories = new ArrayList<>();

    /**
     * Constructs a live telemetry session.
     *
     * @param studentId the associated student identifier
     * @param startTime the local session start time
     */
    public TelemetrySession(String studentId, LocalDateTime startTime) {
        this.studentId = studentId;
        this.startTime = startTime;
        this.events = new ArrayList<>();
    }

    /**
     * Appends a telemetry event to this session.
     *
     * @param event the event to record
     */
    public synchronized void addEvent(TelemetryEvent event) {
        events.add(event);
    }

    /** @return the associated student identifier */
    public String getStudentId() {
        return studentId;
    }

    /** @return the local session start time */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /** @return the local session end time, or {@code null} when live */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Sets the local session end time.
     *
     * @param endTime the time at which the session ends
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns an immutable copy of the session's collected events.
     *
     * @return the collected telemetry events
     */
    public synchronized List<TelemetryEvent> getEvents() {
        return List.copyOf(events);
    }

    /**
     * Creates an independent copy of the session at a capture time.
     * The snapshot copies registered projects and events but does not continue
     * changing when the live session receives later events.
     *
     * @param captureAt the snapshot end time
     * @return a new session copy with the captured state
     * @throws IllegalArgumentException if capture time precedes session start
     */
    public synchronized TelemetrySession snapshot(LocalDateTime captureAt) {

        if (captureAt.isBefore(startTime)) {
            throw new IllegalArgumentException("Snapshot time cannot precede session start.");
        }

        TelemetrySession copy = new TelemetrySession(studentId, startTime);

        for (String directory : projectDirectories) {
            copy.registerProject(directory);
        }

        for (TelemetryEvent event : events) {
            copy.addEvent(event);
        }

        copy.setEndTime(captureAt);

        return copy;
    }

    /**
     * Registers a project directory if it has not already been registered.
     *
     * @param directory the project directory path
     * @throws IllegalArgumentException if the directory is null or blank
     */
    public synchronized void registerProject(String directory) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Project directory must not be blank.");
        }

        if (!projectDirectories.contains(directory)) {
            projectDirectories.add(directory);
        }
    }

    /**
     * Returns an immutable copy of the registered project directories.
     *
     * @return the registered project-directory paths
     */
    public synchronized List<String> getProjectDirectories() {
        return List.copyOf(projectDirectories);
    }

    /**
     * Calculates elapsed session time in seconds.
     *
     * @return the fractional duration, or {@code null} until an end time is set
     */
    public Double getSessionDurationSeconds() {
        if (endTime == null) {
            return null;
        }

        return Duration.between(startTime, endTime).toMillis() / 1000.0;
    }

    /** @return the number of successful compilation events */
    public long getSuccessfulCompilations() {
        return events.stream().filter(event -> event.getType() == EventType.COMPILE_SUCCESS).count();
    }

    /** @return the number of failed compilation events */
    public long getFailedCompilations() {
        return events.stream().filter(event -> event.getType() == EventType.COMPILE_FAILURE).count();
    }

    /** @return the combined successful and failed compilation count */
    public long getTotalCompilations() {
        return getSuccessfulCompilations() + getFailedCompilations();
    }

    /** @return the number of program-run events */
    public long getTotalRuns() {
        return events.stream()
                .filter(event -> 
                        event.getType() == EventType.PROGRAM_RUN)
                .count();
    }
}
