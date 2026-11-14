package tt.uwi;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.MenuItem;

public class TelemetryMenu extends MenuGenerator {

    private final Runnable exportAction;

    public TelemetryMenu(Runnable exportAction) {
        this.exportAction = exportAction;
    }

    @Override
    public MenuItem getToolsMenuItem(BPackage bluejPackage) {
        MenuItem item = new MenuItem("Export telemetry report");

        item.setOnAction(event -> exportAction.run());

        return item;
    }
}