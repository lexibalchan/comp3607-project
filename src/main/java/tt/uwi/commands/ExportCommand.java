package tt.uwi.commands;


import java.io.IOException;

/**
 * Represents an export operation that can be executed by a command invoker.
 */
public interface ExportCommand {

    /**
     * Executes the configured export operation.
     *
     * @throws IOException if the export cannot be completed because of an I/O error
     */
    void execute() throws IOException;
}
