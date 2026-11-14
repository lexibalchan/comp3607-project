package tt.uwi.telemetry.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

public class TelemetrySession {

    private String studentId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<TelemetryEvent> events;

    public TelemetrySession(String studentId, LocalDateTime startTime) {
        this.studentId = studentId;
        this.startTime = startTime;
        this.events = new ArrayList<>();
    }

    public synchronized void addEvent(TelemetryEvent event) {
        events.add(event);
    }

    public String getStudentId() {
        return studentId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public synchronized List<TelemetryEvent> getEvents() {
        return List.copyOf(events);
    }

    public synchronized TelemetrySession snapshot(LocalDateTime captureAt) {

        if (captureAt.isBefore(startTime)) {
            throw new IllegalArgumentException("Snapshot time cannot precede session start.");
        }

        TelemetrySession copy = new TelemetrySession(studentId, startTime);

        for (TelemetryEvent event : events) {
            copy.addEvent(event);
        }

        copy.setEndTime(captureAt);

        return copy;
    }

    public Double getSessionDurationSeconds() {
        if (endTime == null) {
            return null;
        }

        return Duration.between(startTime, endTime).toMillis() / 1000.0;
    }

    public long getSuccessfulCompilations() {
        return events.stream().filter(event -> event.getType() == EventType.COMPILE_SUCCESS).count();
    }

    public long getFailedCompilations() {
        return events.stream().filter(event -> event.getType() == EventType.COMPILE_FAILURE).count();
    }

    public long getTotalCompilations() {
        return getSuccessfulCompilations() + getFailedCompilations();
    }

    public long getTotalRuns() {
        return events.stream()
                .filter(event -> 
                        event.getType() == EventType.PROGRAM_RUN)
                .count();
    }
}