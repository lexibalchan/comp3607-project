package tt.uwi.commands;


import java.io.IOException;

/**
 * Invokes export commands by executing the command supplied to this invoker.
 */
public class CommandInvoker {

    /**
     * Executes an export command.
     *
     * @param command the export command to execute
     * @throws IOException if the command cannot complete because of an I/O error
     */
    public void run(ExportCommand command) throws IOException {
        command.execute();
    }
}
