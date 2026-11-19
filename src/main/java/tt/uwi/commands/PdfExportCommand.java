package tt.uwi.commands;

import java.io.IOException;
import java.nio.file.Path;

import tt.uwi.export.ExportService;
import tt.uwi.export.PdfExportStrategy;
import tt.uwi.telemetry.model.TelemetrySession;

/**
 * Export command that writes a telemetry session using the PDF export strategy.
 */
public class PdfExportCommand implements ExportCommand {

    /** Service responsible for applying the export strategy and writing output. */
    private ExportService exportService;

    /** Telemetry session whose data will be exported. */
    private TelemetrySession session;

    /** Destination path for the generated PDF file. */
    private Path outputFile;

    /**
     * Constructs a PDF export command.
     *
     * @param exportService the export service to configure and invoke
     * @param session the telemetry session to export
     * @param outputFile the destination file path
     */
    public PdfExportCommand(ExportService exportService,
                            TelemetrySession session,
                            Path outputFile) {

        this.exportService = exportService;
        this.session = session;
        this.outputFile = outputFile;
    }

    /**
     * Configures the export service with the PDF strategy and exports the
     * configured telemetry session to the output file.
     *
     * @throws IOException if the PDF export cannot be completed
     */
    @Override
    public void execute() throws IOException {

        exportService.setStrategy(new PdfExportStrategy());

        exportService.export(session, outputFile);
    }
}
