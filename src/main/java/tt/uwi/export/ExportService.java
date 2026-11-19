package tt.uwi.export;

import java.io.IOException;
import java.nio.file.Path;
import tt.uwi.telemetry.model.TelemetrySession;

/**
 * Provides a configurable context for exporting telemetry sessions using
 * an interchangeable export strategy.
 */
public class ExportService {

    /** Strategy that performs the configured export format. */
    private ExportStrategy strategy;

    /**
     * Constructs an export service with an initial strategy.
     *
     * @param strategy the strategy used for subsequent exports
     */
    public ExportService(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Replaces the strategy used by this export service.
     *
     * @param strategy the strategy to use for future exports
     */
    public void setStrategy(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Exports a telemetry session using the currently configured strategy.
     *
     * @param session the telemetry session to export
     * @param outputFile the destination file path
     * @throws IOException if the active strategy cannot write the export
     */
    public void export(TelemetrySession session, Path outputFile)
            throws IOException {

        strategy.export(session, outputFile);
    }
}
