package tt.uwi.export;

import java.io.IOException;
import java.nio.file.Path;
import tt.uwi.telemetry.model.TelemetrySession;

public class ExportService {

    private ExportStrategy strategy;

    public ExportService(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    public void export(TelemetrySession session, Path outputFile)
            throws IOException {

        strategy.export(session, outputFile);
    }
}
