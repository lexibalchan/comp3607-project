package tt.uwi.commands;

import java.io.IOException;
import java.nio.file.Path;

import tt.uwi.export.ExportService;
import tt.uwi.export.JsonExportStrategy;
import tt.uwi.telemetry.model.TelemetrySession;

public class JsonExportCommand implements ExportCommand {

    private ExportService exportService;
    private TelemetrySession session;
    private Path outputFile;

    public JsonExportCommand(ExportService exportService,
                             TelemetrySession session,
                             Path outputFile) {

        this.exportService = exportService;
        this.session = session;
        this.outputFile = outputFile;
    }

    @Override
    public void execute() throws IOException {

        exportService.setStrategy(new JsonExportStrategy());

        exportService.export(session, outputFile);
    }
}
