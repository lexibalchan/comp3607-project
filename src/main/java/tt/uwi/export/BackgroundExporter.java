package tt.uwi.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;

import tt.uwi.telemetry.model.TelemetrySession;
import tt.uwi.commands.CommandInvoker;
import tt.uwi.commands.JsonExportCommand;
import tt.uwi.commands.PdfExportCommand;

/**
 * Executes telemetry snapshot exports asynchronously and writes matching JSON
 * and PDF files to a configured output directory.
 */
public class BackgroundExporter implements AutoCloseable {

    /** Directory where exported JSON and PDF files are written. */
    private final Path outputDirectory;

    /** Single-thread executor that processes export requests sequentially. */
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor();

    /**
     * Constructs a background exporter.
     *
     * @param outputDirectory directory where exported files will be written
     */
    public BackgroundExporter(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Schedules asynchronous JSON and PDF exports for a completed snapshot.
     * Both files use a shared timestamped, UUID-suffixed base name.
     *
     * @param snapshot the completed telemetry-session snapshot to export
     * @return a future that completes after both export files are written
     * @throws IllegalArgumentException if the snapshot has no end time
     */
    public Future<?> exportSnapshot(TelemetrySession snapshot) {
        if (snapshot.getEndTime() == null) {
            throw new IllegalArgumentException(
                    "Export requires a snapshot with an end time."
            );
        }

        return worker.submit(() -> {
            try {
                Files.createDirectories(outputDirectory);

                String timestamp = snapshot.getEndTime().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                );

                String baseName = projectLabel(snapshot) + "-" + timestamp + "-" + UUID.randomUUID();

                Path jsonFile =
                        outputDirectory.resolve(baseName + ".json");
                Path pdfFile =
                        outputDirectory.resolve(baseName + ".pdf");

                ExportService service = new ExportService(new JsonExportStrategy());
                CommandInvoker invoker = new CommandInvoker();

                invoker.run(new JsonExportCommand(service, snapshot, jsonFile));
                System.out.println("Telemetry JSON saved to: " + jsonFile);


                invoker.run(new PdfExportCommand(service, snapshot, pdfFile));
                System.out.println("Telemetry PDF saved to: " + pdfFile);

            } catch (IOException | RuntimeException exception) {
                System.err.println(
                        "Telemetry export failed: " + exception.getMessage()
                );
                throw new IllegalStateException(
                        "Could not complete telemetry export.", exception
                );
            }
        });
    }

    /**
     * Builds a sanitized filename label from the snapshot's registered projects.
     * Uses fixed labels for zero or multiple projects and the folder name for
     * exactly one project.
     *
     * @param snapshot the telemetry snapshot containing project directories
     * @return a filename-safe project label of at most 60 characters
     */
    private String projectLabel(TelemetrySession snapshot) {
        var directories = snapshot.getProjectDirectories();

        if (directories.isEmpty()) {
            return "no-project";
        }

        if (directories.size() > 1) {
            return "multiple-projects";
        }

        Path folderName = Path.of(directories.get(0)).getFileName();

        String name = folderName == null ? "project" : folderName.toString();

        String safeName = name.replaceAll("[^a-zA-Z0-9._-]", "_");

        return safeName.substring(0, Math.min(safeName.length(), 60));
    }

    /**
     * Shuts down the export worker and waits up to 30 seconds for pending
     * exports to finish. An interruption restores the thread's interrupted
     * status and is reported to standard error.
     */
    @Override
    public void close() {
        worker.shutdown();

        try {
            if (!worker.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println(
                        "Telemetry exports are still pending after 30 seconds."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println(
                    "Interrupted while waiting for telemetry exports."
            );
        }
    }
}
