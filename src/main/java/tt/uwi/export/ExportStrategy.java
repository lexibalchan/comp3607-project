package tt.uwi.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Path;

import tt.uwi.telemetry.model.TelemetrySession;

import com.fasterxml.jackson.databind.node.ObjectNode;
import tt.uwi.analytics.SessionAnalytics;

import tt.uwi.analytics.ActivityAnalytics;

/**
 * Exports telemetry sessions as formatted JSON, enriched with compilation
 * interval and editor-activity analytics.
 */
public class JsonExportStrategy implements ExportStrategy {

    /**
     * Writes the session data and derived analytics as an indented JSON document.
     * The editor-activity section describes edit events and estimates active and
     * idle time using a 30-second threshold.
     *
     * @param session the telemetry session to export
     * @param outputFile the destination JSON file
     * @throws IOException if the JSON document cannot be written
     */
    @Override
    public void export(TelemetrySession session, Path outputFile)
            throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        ObjectNode document = mapper.valueToTree(session);

        document.set("compileIntervalsSeconds", mapper.valueToTree(
            new SessionAnalytics().calculateCompileIntervalsSeconds(session)
        ));

        var activity = new ActivityAnalytics().analyse(session, 30);

        ObjectNode activityNode = document.putObject("editorActivity");

        activityNode.put(
                "measurement",
                "Document-change events; not individual keystrokes"
        );
        activityNode.put(
                "estimationRule",
                "Each edit marks the next 30 seconds active; "
                        + "overlaps count once; capped at session end"
        );
        activityNode.put("idleThresholdSeconds", 30);
        activityNode.put("editEvents", activity.editEvents());
        activityNode.put(
                "estimatedActiveSeconds",
                Math.round(activity.estimatedActiveSeconds() * 1000.0) / 1000.0
        );
        activityNode.put(
                "estimatedIdleSeconds",
                Math.round(activity.estimatedIdleSeconds() * 1000.0) / 1000.0
        );

        mapper.writeValue(outputFile.toFile(), document);
    }
}
