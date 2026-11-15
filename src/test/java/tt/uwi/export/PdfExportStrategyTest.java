package tt.uwi.export;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;

public class PdfExportStrategyTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsTwoPagesWithClassAndMethodTimeline() throws Exception {
        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        session.addEvent(new TelemetryEvent(
                EventType.CLASS_CREATED,
                start.plusSeconds(10),
                "Detected by class-list comparison",
                "Course"
        ));

		session.addEvent(new TelemetryEvent(
				EventType.METHOD_ADDED,
				start.plusSeconds(20),
				"Detected method addition: probe(): void"
						+ " | Package directory: test-project",
				"Course"
		));

		session.addEvent(new TelemetryEvent(
				EventType.METHOD_REMOVED,
				start.plusSeconds(30),
				"Detected method removal: probe(): void"
						+ " | Package directory: test-project",
				"Course"
		));
	
        session.setEndTime(start.plusSeconds(60));

        Path outputFile =
                temporaryDirectory.resolve("timeline-report.pdf");

        new PdfExportStrategy().export(session, outputFile);

        try (PDDocument document =
                     PDDocument.load(outputFile.toFile())) {

            assertEquals(2, document.getNumberOfPages());

            PDFTextStripper reader = new PDFTextStripper();
            reader.setStartPage(2);
            reader.setEndPage(2);

            String timelineText = reader.getText(document);

			assertTrue(timelineText.contains("Class and method progression timeline"));
			assertTrue(timelineText.contains("Course - Method added: probe(): void"));
			assertTrue(timelineText.contains("Course - Method removed: probe(): void"));
			assertFalse(timelineText.contains("Package directory:"));
            assertTrue(timelineText.contains("Course"));
            assertTrue(timelineText.contains("2026-11-13 10:00:10"));
            assertTrue(timelineText.contains("Showing 3 of 3"));
        }
    }

    @Test
    void crowdedReportStaysWithinTwoPagesAndDisclosesLimits()
            throws Exception {

        LocalDateTime start =
                LocalDateTime.of(2026, 11, 13, 10, 0);

        TelemetrySession session =
                new TelemetrySession("TEST-STUDENT", start);

        for (int index = 0; index < 20; index++) {
            session.addEvent(new TelemetryEvent(
                    EventType.CLASS_CREATED,
                    start.plusSeconds(index),
                    "Detected by class-list comparison",
                    "ExampleClass" + index
            ));
        }

        for (int index = 0; index < 7; index++) {
            session.addEvent(new TelemetryEvent(
                    EventType.COMPILE_ERROR,
                    start.plusSeconds(25 + index),
                    "Error " + index + ": " + "long diagnostic ".repeat(15),
                    null,
                    index + 1
            ));
        }

        session.addEvent(new TelemetryEvent(
                EventType.COMPILE_FAILURE,
                start.plusSeconds(35),
                "Compilation failed",
                null
        ));

        session.addEvent(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY,
                start.plusSeconds(40),
                "Editor document changed",
                "ExampleClass0"
        ));

        session.setEndTime(start.plusSeconds(100));

        Path outputFile =
                temporaryDirectory.resolve("crowded-report.pdf");

        new PdfExportStrategy().export(session, outputFile);

        try (PDDocument document =
                    PDDocument.load(outputFile.toFile())) {

            assertEquals(2, document.getNumberOfPages());

            PDFTextStripper reader = new PDFTextStripper();
            reader.setStartPage(1);
            reader.setEndPage(1);

            String firstPage = reader.getText(document);

            assertTrue(firstPage.contains("Showing 5 of 7"));
            assertTrue(firstPage.contains("..."));
            assertTrue(firstPage.contains("active 30.00 s"));
            assertTrue(firstPage.contains("idle 70.00 s"));

            reader.setStartPage(2);
            reader.setEndPage(2);

            String secondPage = reader.getText(document);

            assertTrue(secondPage.contains("Showing 18 of 20"));
            assertTrue(secondPage.contains("ExampleClass17"));
            assertFalse(secondPage.contains("ExampleClass18"));
            assertFalse(secondPage.contains("ExampleClass19"));
        }
    }
}