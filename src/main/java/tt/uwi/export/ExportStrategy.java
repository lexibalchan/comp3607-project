package tt.uwi.export;

import java.io.IOException;
import java.nio.file.Path;
import tt.uwi.telemetry.model.TelemetrySession;

/**
 * Strategy contract for exporting a telemetry session to a destination file.
 * Concrete implementations define the output format and serialization details.
 */
public interface ExportStrategy {

    /**
     * Exports a telemetry session using this strategy's format.
     *
     * @param session the telemetry session to export
     * @param outputFile the destination file path
     * @throws IOException if the export destination cannot be written
     */
    void export(TelemetrySession session, Path outputFile)
            throws IOException;
}
