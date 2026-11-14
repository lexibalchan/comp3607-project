package tt.uwi.export;

import java.io.IOException;
import java.nio.file.Path;
import tt.uwi.telemetry.model.TelemetrySession;

public interface ExportStrategy {

    void export(TelemetrySession session, Path outputFile)
            throws IOException;
}