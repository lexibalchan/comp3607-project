package tt.uwi.commands;

import java.io.IOException;
import java.nio.file.Path;

import tt.uwi.export.ExportService;
import tt.uwi.export.PdfExportStrategy;
import tt.uwi.telemetry.model.TelemetrySession;

public class PdfExportCommand implements ExportCommand {

    private ExportService exportService;
    private TelemetrySession session;
    private Path outputFile;

    public PdfExportCommand(ExportService exportService,
                            TelemetrySession session,
                            Path outputFile) {

        this.exportService = exportService;
        this.session = session;
        this.outputFile = outputFile;
    }

    @Override
    public void execute() throws IOException {

        exportService.setStrategy(new PdfExportStrategy());

        exportService.export(session, outputFile);
    }
}