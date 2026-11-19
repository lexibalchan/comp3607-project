package tt.uwi;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.MenuItem;

/**
 * Provides the BlueJ Tools menu entry that queues a telemetry export.
 */
public class TelemetryMenu extends MenuGenerator {

    /** Action invoked when the export menu item is selected. */
    private final Runnable exportAction;

    /**
     * Constructs the telemetry menu generator.
     *
     * @param exportAction the action to run when the menu item is selected
     */
    public TelemetryMenu(Runnable exportAction) {
        this.exportAction = exportAction;
    }

    /**
     * Creates the Tools menu item for exporting the current telemetry session.
     * Selecting the item invokes the configured export action.
     *
     * @param bluejPackage the currently selected package, if provided by BlueJ
     * @return the menu item for exporting telemetry
     */
    @Override
    public MenuItem getToolsMenuItem(BPackage bluejPackage) {
        MenuItem item = new MenuItem("Export telemetry report");

        item.setOnAction(event -> exportAction.run());

        return item;
    }
}
