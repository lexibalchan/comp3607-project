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

public class PdfExportStrategy implements ExportStrategy {

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
    

    private void addClassTimelinePage(
        PDDocument document, TelemetrySession session)
        throws IOException {

        PDPage page = new PDPage();
        document.addPage(page);

        var creations = session.getEvents().stream()
                .filter(event -> event.getType() == EventType.CLASS_CREATED)
                .sorted(Comparator.comparing(TelemetryEvent::getTimestamp))
                .toList();

        DateTimeFormatter format =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (PDPageContentStream content =
                    new PDPageContentStream(document, page)) {

            writeText(content, 50, 740, 16,
                    "Class creation timeline", 512);

            writeText(content, 50, 712, 10,
                    "Times show detection at inventory scans, not exact creation.",
                    512);

            writeText(content, 50, 694, 10,
                    "Classes present at the initial scan are not listed.", 512);

            writeText(content, 60, 658, 10, "Detected at (local time)", 145);
            writeText(content, 220, 658, 10, "Class name", 332);

            content.moveTo(50, 648);
            content.lineTo(562, 648);
            content.stroke();

            int displayed = Math.min(creations.size(), 18);
            float y = 628;

            if (creations.isEmpty()) {
                writeText(content, 60, y, 10,
                        "No class creations detected in this session.", 492);
            }

            for (int index = 0; index < displayed; index++) {
                TelemetryEvent event = creations.get(index);

                writeText(content, 60, y, 9,
                        format.format(event.getTimestamp()), 145);

                writeText(content, 220, y, 10,
                        event.getClassName(), 332);

                content.moveTo(50, y - 8);
                content.lineTo(562, y - 8);
                content.stroke();

                y -= 24;
            }

            writeText(content, 50, 150, 10,
                    "Showing " + displayed + " of " + creations.size()
                            + " detected creations.", 512);

            writeText(content, 50, 132, 10,
                    "Long labels are shortened; full event details remain in JSON.",
                    512);

            writeText(content, 50, 50, 9, "Page 2 of 2", 512);
        }
    }

    private String safe(String value) {
        return value == null
                ? "Not supplied"
                : value.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
