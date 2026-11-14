package tt.uwi.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityExportPrecisionTest {

    @TempDir
    Path directory;

    @Test
    void preservesFractionalActivitySeconds() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 11, 13, 10, 0);
        TelemetrySession session = new TelemetrySession("TEST", start);

        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY,
                start.plusSeconds(50).plusNanos(125_000_000),
                "Editor document changed",
                "Learner"
        ));

        session.setEndTime(
                start.plusSeconds(60).plusNanos(625_000_000)
        );

        Path output = directory.resolve("precision.json");
        new JsonExportStrategy().export(session, output);

        JsonNode activity = new ObjectMapper()
                .readTree(output.toFile())
                .get("editorActivity");

        assertEquals(10.5,
                activity.get("estimatedActiveSeconds").asDouble(), 0.001);
        assertEquals(50.125,
                activity.get("estimatedIdleSeconds").asDouble(), 0.001);
    }
}