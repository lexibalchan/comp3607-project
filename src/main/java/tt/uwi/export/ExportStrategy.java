package tt.uwi.export;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import tt.uwi.analytics.AnalyticsSummary;
import tt.uwi.analytics.SessionAnalytics;
import tt.uwi.analytics.ActivityAnalytics;
import tt.uwi.analytics.ActivitySummary;
import tt.uwi.telemetry.model.TelemetrySession;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Exports telemetry sessions as a two-page PDF report containing compilation,
 * activity, error-frequency, and class/method progression information.
 */
public class PdfExportStrategy implements ExportStrategy {

    /**
     * Draws the active-versus-idle chart and its estimation notes.
     *
     * @param content the PDF content stream being drawn
     * @param activity the calculated editor activity summary
     * @param x the chart's left coordinate
     * @param y the chart's upper coordinate
     * @param width the chart width
     * @throws IOException if PDF content cannot be written
     */
    private void drawActivityChart(
            PDPageContentStream content,
            ActivitySummary activity,
            float x, float y, float width) throws IOException {

        double active = activity.estimatedActiveSeconds();
        double idle = activity.estimatedIdleSeconds();
        double total = active + idle;

        writeText(content, x, y, 13,
                "Estimated active versus idle time", width);

        writeText(content, x, y - 20, 10,
                "Document-change events: " + activity.editEvents(), width);

        if (total > 0) {
            float activeWidth = (float) (width * active / total);

            content.setNonStrokingColor(35, 120, 170);
            content.addRect(x, y - 48, activeWidth, 16);
            content.fill();

            content.setNonStrokingColor(210, 215, 220);
            content.addRect(
                    x + activeWidth,
                    y - 48,
                    width - activeWidth,
                    16
            );
            content.fill();

            content.setNonStrokingColor(0, 0, 0);
        } else {
            writeText(content, x, y - 42, 10,
                    "No elapsed session time to chart.", width);
        }

        writeText(content, x, y - 68, 10,
                String.format(
                        java.util.Locale.ROOT,
                        "Blue: active %.2f s | Grey: idle %.2f s",
                        active, idle
                ), width);

        writeText(content, x, y - 88, 9,
                "Estimate: 30 seconds after each edit; overlapping windows merged.",
                width);

        writeText(content, x, y - 104, 9,
                "Idle means outside edit windows, not necessarily inactivity.",
                width);
    }

    /**
     * Draws the top compiler-error messages as a ranked horizontal bar chart.
     *
     * @param content the PDF content stream being drawn
     * @param errorCounts compiler-error descriptions mapped to occurrence counts
     * @param x the chart's left coordinate
     * @param y the chart's upper coordinate
     * @param width the chart width
     * @throws IOException if PDF content cannot be written
     */
    private void drawErrorChart(
            PDPageContentStream content,
            Map<String, Integer> errorCounts,
            float x, float y, float width) throws IOException {

        writeText(content, x, y, 13,
                "Compilation error frequency", width);

        if (errorCounts.isEmpty()) {
            writeText(content, x, y - 24, 10,
                    "No compiler errors recorded.", width);
            return;
        }

        var ranked = errorCounts.entrySet().stream()
                .sorted(
                        Comparator
                                .<Map.Entry<String, Integer>>
                                        comparingInt(entry -> entry.getValue())
                                .reversed()
                                .thenComparing(entry -> entry.getKey())
                )
                .toList();

        int displayed = Math.min(ranked.size(), 5);
        int maximumCount = ranked.get(0).getValue();

        float maximumBarWidth = width - 50;

        for (int index = 0; index < displayed; index++) {
            var entry = ranked.get(index);
            float rowY = y - 26 - index * 40;

            writeText(content, x, rowY, 9,
                    entry.getKey(), width);

            float barWidth = maximumBarWidth
                    * entry.getValue() / maximumCount;

            content.setNonStrokingColor(190, 75, 55);
            content.addRect(x, rowY - 18, barWidth, 10);
            content.fill();

            content.setNonStrokingColor(0, 0, 0);

            writeText(content, x + maximumBarWidth + 8,
                    rowY - 18, 9,
                    Integer.toString(entry.getValue()), 42);
        }

        float noteY = y - 26 - displayed * 40;

        writeText(content, x, noteY, 9,
                "Showing " + displayed + " of " + ranked.size()
                        + " distinct messages, ranked by frequency.",
                width);

        writeText(content, x, noteY - 16, 9,
                "Long labels are shortened; full messages remain in JSON.",
                width);
    }

	/**
	 * Determines whether an event belongs in the class/method timeline.
	 *
	 * @param event the telemetry event to inspect
	 * @return {@code true} when the event is a class or method change
	 */
	private boolean isTimelineEvent(TelemetryEvent event) {
		return switch (event.getType()) {
			case CLASS_CREATED, CLASS_DELETED, CLASS_RENAMED,
				METHOD_ADDED, METHOD_REMOVED -> true;
			default -> false;
		};
	}

	/**
	 * Creates a concise timeline label for a class or method change event.
	 *
	 * @param event the timeline event
	 * @return a human-readable label for PDF display
	 */
	private String timelineLabel(TelemetryEvent event) {
		String className = safe(event.getClassName());

		return switch (event.getType()) {
			case CLASS_CREATED -> "Class added: " + className;
			case CLASS_DELETED -> "Class deleted: " + className;
			case CLASS_RENAMED -> safe(event.getDescription());

			case METHOD_ADDED, METHOD_REMOVED -> {
				String description = safe(event.getDescription());

				int directoryStart =
						description.indexOf(" | Package directory:");

				if (directoryStart >= 0) {
					description = description.substring(0, directoryStart);
				}

				description = description
						.replace("Detected method addition: ", "Method added: ")
						.replace("Detected method removal: ", "Method removed: ");

				yield className + " - " + description;
			}

			default -> className;
		};
	}

    /**
     * Creates and saves a two-page PDF report for a telemetry session.
     *
     * @param session the telemetry session to export
     * @param outputFile the destination PDF file
     * @throws IOException if the PDF document cannot be created or written
     */
    @Override
    public void export(TelemetrySession session, Path outputFile)
            throws IOException {

        SessionAnalytics analytics = new SessionAnalytics();
        AnalyticsSummary summary = analytics.analyse(session);

        var activity = new ActivityAnalytics().analyse(session, 30);
        var errorCounts = analytics.countErrorsByMessage(session);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content =
                        new PDPageContentStream(document, page)) {

                writeText(content, 50, 740, 16,
                        "BlueJ Telemetry Session Report", 512);

                writeText(content, 50, 714, 10,
                        "Student ID: " + safe(session.getStudentId()), 512);

                DateTimeFormatter format =
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                writeText(content, 50, 694, 10,
                        "Session start (local): "
                                + format.format(session.getStartTime()),
                        512);

                writeText(content, 50, 674, 10,
                        String.format(
                                java.util.Locale.ROOT,
                                "Session duration: %.2f seconds",
                                session.getSessionDurationSeconds()
                        ), 512);

                writeText(content, 50, 654, 10,
                        "Total events: " + summary.getTotalEvents()
                                + " | Completed invocations: "
                                + summary.getProgramRuns(),
                        512);

                writeText(content, 50, 634, 10,
                        "Class creations detected: "
                                + summary.getClassesCreated(),
                        512);

                writeText(content, 50, 604, 13,
                        "Compilation outcomes", 512);

                writeText(content, 60, 582, 10, "Outcome", 300);
                writeText(content, 450, 582, 10, "Count", 102);

                content.moveTo(50, 575);
                content.lineTo(562, 575);
                content.stroke();

                writeText(content, 60, 560, 10, "Successful", 300);
                writeText(content, 450, 560, 10,
                        Integer.toString(
                                summary.getCompilationAttempts()
                                        - summary.getCompilationFailures()
                        ), 102);

                writeText(content, 60, 542, 10, "Failed", 300);
                writeText(content, 450, 542, 10,
                        Integer.toString(summary.getCompilationFailures()),
                        102);

                writeText(content, 60, 524, 10, "Total completed", 300);
                writeText(content, 450, 524, 10,
                        Integer.toString(summary.getCompilationAttempts()),
                        102);

                content.moveTo(50, 516);
                content.lineTo(562, 516);
                content.stroke();

                writeText(content, 50, 498, 10,
                        String.format(
                                java.util.Locale.ROOT,
                                "Compilation success rate: %.2f%%",
                                summary.getCompilationSuccessRate()
                        ), 512);

                drawActivityChart(content, activity, 50, 470, 512);
                drawErrorChart(content, errorCounts, 50, 330, 512);

                writeText(content, 50, 50, 9, "Page 1 of 2", 512);
            }

            addClassTimelinePage(document, session);

            document.save(outputFile.toFile());
        }
    }

    /**
     * Writes text with a Helvetica font and shortens it when it exceeds the
     * configured maximum width.
     *
     * @param content the PDF content stream being drawn
     * @param x the text's starting horizontal coordinate
     * @param y the text's starting vertical coordinate
     * @param size the font size
     * @param value the text to write
     * @param maximumWidth the available text width
     * @throws IOException if PDF content cannot be written
     */
    private void writeText(
            PDPageContentStream content,
            float x, float y, float size,
            String value, float maximumWidth) throws IOException {

        String text = safe(value);

        if (PDType1Font.HELVETICA.getStringWidth(text)
                / 1000f * size > maximumWidth) {

            while (!text.isEmpty()
                    && PDType1Font.HELVETICA.getStringWidth(text + "...")
                    / 1000f * size > maximumWidth) {
                text = text.substring(0, text.length() - 1);
            }

            text += "...";
        }

        content.beginText();
        content.setFont(PDType1Font.HELVETICA, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }
    

    /**
     * Adds the class and method progression timeline as the second PDF page.
     *
     * @param document the PDF document receiving the page
     * @param session the telemetry session containing timeline events
     * @throws IOException if PDF content cannot be written
     */
    private void addClassTimelinePage(
        PDDocument document, TelemetrySession session)
        throws IOException {

        PDPage page = new PDPage();
        document.addPage(page);

        var changes = session.getEvents().stream()
                .filter(this::isTimelineEvent)
                .sorted(Comparator.comparing(TelemetryEvent::getTimestamp))
                .toList();

        DateTimeFormatter format =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (PDPageContentStream content =
                    new PDPageContentStream(document, page)) {

            writeText(content, 50, 740, 16,
                    "Class and method progression timeline", 512);

            writeText(content, 50, 712, 10,
                    "Times show recorded notifications or detected inventory changes.",
                    512);

            writeText(content, 50, 694, 10,
                    "Initial class and method inventories establish a baseline.", 512);

            writeText(content, 60, 658, 10, "Detected at (local time)", 145);
            writeText(content, 220, 658, 10, "Change detected", 332);

            content.moveTo(50, 648);
            content.lineTo(562, 648);
            content.stroke();

            int displayed = Math.min(changes.size(), 18);
            float y = 628;

            if (changes.isEmpty()) {
                writeText(content, 60, y, 10,
                        "No class or method changes detected in this session.", 492);
            }

            for (int index = 0; index < displayed; index++) {
                TelemetryEvent event = changes.get(index);

                writeText(content, 60, y, 9,
                        format.format(event.getTimestamp()), 145);

                writeText(content, 220, y, 9,
                        timelineLabel(event), 332);

                content.moveTo(50, y - 8);
                content.lineTo(562, y - 8);
                content.stroke();

                y -= 24;
            }

            writeText(content, 50, 150, 10,
                    "Showing " + displayed + " of " + changes.size()
                            + " detected changes.", 512);

            writeText(content, 50, 132, 10,
                    "Long labels are shortened; full event details remain in JSON.",
                    512);

            writeText(content, 50, 50, 9, "Page 2 of 2", 512);
        }
    }

    /**
     * Converts null or non-printable text into a PDF-safe display string.
     *
     * @param value the text to normalize
     * @return "Not supplied" for null text, otherwise printable-ASCII text
     */
    private String safe(String value) {
        return value == null
                ? "Not supplied"
                : value.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
