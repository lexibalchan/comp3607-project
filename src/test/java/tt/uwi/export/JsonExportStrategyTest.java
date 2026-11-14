package tt.uwi.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;

public class JsonExportStrategyTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsSessionMetadataEventsAndIntervals()
            throws IOException {

        LocalDateTime start =
                LocalDateTime.of(2026, 11, 10, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED, start,
                "Compilation started", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_SUCCESS, start.plusSeconds(1),
                "Compilation succeeded", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_STARTED, start.plusSeconds(12),
                "Compilation started", null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_ERROR, start.plusSeconds(13),
                "Cannot find \"name\"\nCheck declaration",
                "Student.java", 11
        ));

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_FAILURE, start.plusSeconds(14),
                "Compilation failed", null
        ));

        session.setEndTime(start.plusSeconds(20));

        Path outputFile = temporaryDirectory.resolve("session.json");
        new JsonExportStrategy().export(session, outputFile);

        JsonNode document =
                new ObjectMapper().readTree(outputFile.toFile());

        assertEquals("TEST-STUDENT",
                document.path("studentId").asText());
        assertEquals(20.0,
                document.path("sessionDurationSeconds").asDouble(),
                0.001);

        assertEquals(2, document.path("totalCompilations").asInt());
        assertEquals(1, document.path("successfulCompilations").asInt());
        assertEquals(1, document.path("failedCompilations").asInt());

        assertTrue(document.path("startTime").isTextual());
        assertEquals(start,
                LocalDateTime.parse(document.path("startTime").asText()));

        JsonNode events = document.path("events");
        assertEquals(5, events.size());
        assertEquals("COMPILE_ERROR",
                events.path(3).path("type").asText());
        assertEquals("Cannot find \"name\"\nCheck declaration",
                events.path(3).path("description").asText());
        assertEquals(11, events.path(3).path("lineNumber").asInt());

        JsonNode intervals = document.path("compileIntervalsSeconds");
        assertTrue(intervals.isArray());
        assertEquals(1, intervals.size());
        assertEquals(12.0, intervals.path(0).asDouble(), 0.001);
    }

    @Test
    void exportsExecutionDetailsAndPreservesMissingMethodName()
            throws IOException {

        LocalDateTime time =
                LocalDateTime.of(2026, 11, 13, 9, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", time);

        session.addEvent(TelemetryEvent.execution(
                EventType.PROGRAM_RUN,
                time,
                "Invocation finished",
                "Learner",
                "successfulRun",
                "NORMAL_EXIT"
        ));

        session.addEvent(TelemetryEvent.execution(
                EventType.PROGRAM_RUN,
                time.plusSeconds(5),
                "Invocation finished with an exception",
                "Learner",
                null,
                "EXCEPTION_EXIT"
        ));

        session.setEndTime(time.plusSeconds(10));

        Path outputFile =
                temporaryDirectory.resolve("execution-session.json");

        new JsonExportStrategy().export(session, outputFile);

        JsonNode document =
                new ObjectMapper().readTree(outputFile.toFile());

        assertEquals(2, document.path("totalRuns").asInt());

        JsonNode events = document.path("events");
        assertEquals(2, events.size());

        JsonNode successful = events.path(0);

        assertEquals("Learner", successful.path("className").asText());
        assertEquals("successfulRun",
                successful.path("methodName").asText());
        assertEquals("NORMAL_EXIT",
                successful.path("executionOutcome").asText());

        JsonNode exceptional = events.path(1);

        assertTrue(exceptional.path("methodName").isNull());
        assertEquals("EXCEPTION_EXIT",
                exceptional.path("executionOutcome").asText());
    }

    @Test
    void exportsEditorActivityEstimatesAndThreshold()
            throws IOException {

        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY, start.plusSeconds(10),
                "Editor document changed", "Learner"
        ));

        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY, start.plusSeconds(20),
                "Editor document changed", "Learner"
        ));

        session.setEndTime(start.plusSeconds(100));

        Path outputFile =
                temporaryDirectory.resolve("activity-session.json");

        new JsonExportStrategy().export(session, outputFile);

        JsonNode document =
                new ObjectMapper().readTree(outputFile.toFile());

        JsonNode activity = document.path("editorActivity");

        assertTrue(activity.isObject());
        assertEquals(30, activity.path("idleThresholdSeconds").asInt());
        assertEquals(2, activity.path("editEvents").asInt());

        assertEquals(
                40.0,
                activity.path("estimatedActiveSeconds").asDouble(),
                0.001
        );

        assertEquals(
                60.0,
                activity.path("estimatedIdleSeconds").asDouble(),
                0.001
        );

        assertFalse(activity.path("estimationRule").asText().isBlank());
    }
}