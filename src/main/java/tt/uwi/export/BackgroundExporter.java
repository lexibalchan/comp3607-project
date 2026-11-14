package tt.uwi.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import tt.uwi.telemetry.model.TelemetrySession;
import tt.uwi.commands.CommandInvoker;
import tt.uwi.commands.JsonExportCommand;
import tt.uwi.commands.PdfExportCommand;

public class BackgroundExporter implements AutoCloseable {

    private final Path outputDirectory;

    private final ExecutorService worker =
            Executors.newSingleThreadExecutor();

    public BackgroundExporter(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Future<?> exportSnapshot(TelemetrySession snapshot) {
        if (snapshot.getEndTime() == null) {
            throw new IllegalArgumentException(
                    "Export requires a snapshot with an end time."
            );
        }

        return worker.submit(() -> {
            try {
                Files.createDirectories(outputDirectory);

                String baseName = "session-" + UUID.randomUUID();

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