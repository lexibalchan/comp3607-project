package tt.uwi.export;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;

public class BackgroundExporterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesReadableJsonAndPdfFromSnapshot() throws Exception {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.registerProject(temporaryDirectory.resolve("TelemetryTest").toString());

        TelemetrySession snapshot =
                session.snapshot(start.plusSeconds(10));

        try (BackgroundExporter exporter =
                     new BackgroundExporter(temporaryDirectory)) {

            exporter.exportSnapshot(snapshot).get(10, TimeUnit.SECONDS);
        }

        Path jsonFile;
        Path pdfFile;

        try (var files = Files.list(temporaryDirectory)) {
            var exported = files.toList();

            assertEquals(2, exported.size());

            jsonFile = exported.stream()
                    .filter(path -> path.toString().endsWith(".json"))
                    .findFirst()
                    .orElseThrow();

            pdfFile = exported.stream()
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .findFirst()
                    .orElseThrow();
        }

        String jsonName = jsonFile.getFileName().toString();
        String pdfName = pdfFile.getFileName().toString();

        assertTrue(jsonName.startsWith("TelemetryTest-20261113-100010-"));

        assertEquals(jsonName.substring(0, jsonName.length() - "json".length()), 
                    pdfName.substring(0, pdfName.length() - "pdf".length()));

        var json = new ObjectMapper().readTree(jsonFile.toFile());

        assertEquals("TEST-STUDENT", json.path("studentId").asText());

        assertEquals(session.getProjectDirectories().get(0), 
                    json.path("projectDirectories").get(0).asText());

        assertEquals(
                10.0,
                json.path("sessionDurationSeconds").asDouble(),
                0.001
        );

        try (PDDocument pdf = PDDocument.load(pdfFile.toFile())) {
            assertTrue(pdf.getNumberOfPages() >= 1);
        }

        assertNull(session.getEndTime());
    }

    @Test
    void labelsExportWithNoRegisteredProject() throws Exception {
        assertExportLabel("no-project");
    }

    @Test
    void labelsExportContainingMultipleProjects() throws Exception {
        assertExportLabel(
                "multiple-projects",
                "project-one",
                "project-two"
        );
    }

    private void assertExportLabel(
            String expectedLabel,
            String... projectDirectories) throws Exception {

        LocalDateTime start =
                LocalDateTime.of(2026, 11, 15, 10, 0);

        TelemetrySession session = new TelemetrySession("TEST", start);

        for (String directory : projectDirectories) {
            session.registerProject(directory);
        }

        try (BackgroundExporter exporter =
                    new BackgroundExporter(temporaryDirectory)) {

            exporter.exportSnapshot(
                    session.snapshot(start.plusSeconds(10))
            ).get(10, TimeUnit.SECONDS);
        }

        try (var files = Files.list(temporaryDirectory)) {
            var exported = files.toList();

            assertEquals(2, exported.size());

            for (Path file : exported) {
                assertTrue(
                        file.getFileName().toString().startsWith(
                                expectedLabel + "-20261115-100010-"
                        )
                );
            }
        }
    }
}