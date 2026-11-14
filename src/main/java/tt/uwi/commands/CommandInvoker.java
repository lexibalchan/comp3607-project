package tt.uwi.commands;


import java.io.IOException;

public class CommandInvoker {

    public void run(ExportCommand command) throws IOException {
        command.execute();
    }
}
