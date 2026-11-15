package tt.uwi.telemetry.model;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetrySessionProjectTest {

    @Test
    void registersEachProjectDirectoryOnce() {
        TelemetrySession session = new TelemetrySession(
                "TEST",
                LocalDateTime.of(2026, 11, 15, 10, 0)
        );

        session.registerProject("C:/Projects/Example");
        session.registerProject("C:/Projects/Example");

        assertEquals(
                List.of("C:/Projects/Example"),
                session.getProjectDirectories()
        );
    }

    @Test
    void snapshotKeepsProjectDirectoriesAtCaptureTime() {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 15, 10, 0);

        TelemetrySession session = new TelemetrySession("TEST", start);
        session.registerProject("C:/Projects/First");

        TelemetrySession snapshot =
                session.snapshot(start.plusSeconds(10));

        session.registerProject("C:/Projects/Second");

        assertEquals(
                List.of("C:/Projects/First"),
                snapshot.getProjectDirectories()
        );

        assertEquals(
                List.of("C:/Projects/First", "C:/Projects/Second"),
                session.getProjectDirectories()
        );
    }
}