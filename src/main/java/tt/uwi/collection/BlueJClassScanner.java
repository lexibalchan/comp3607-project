package tt.uwi.collection;

import java.util.HashSet;
import java.util.Set;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;

/**
 * Scans BlueJ projects and packages to synchronize class inventory and editor
 * listener registrations.
 */
public class BlueJClassScanner {

    /** The BlueJ environment used to access open projects. */
    private final BlueJ bluej;

    /** Tracks class names and publishes detected inventory changes. */
    private final ClassInventoryTracker tracker;

    /** Manages editor listeners attached to classes in scanned packages. */
    private final EditorListenerRegistry editorRegistry;

    /**
     * Constructs a class scanner with its BlueJ environment and supporting
     * inventory and listener components.
     *
     * @param bluej the BlueJ environment
     * @param tracker the class inventory tracker
     * @param editorRegistry the registry for editor listeners
     */
    public BlueJClassScanner(
            BlueJ bluej, ClassInventoryTracker tracker, EditorListenerRegistry editorRegistry) {
        this.bluej = bluej;
        this.tracker = tracker;
        this.editorRegistry = editorRegistry;
    }

    /**
     * Scans a package and updates its class inventory and editor listeners.
     * Package access failures are written to standard error and do not alter
     * the existing package inventory.
     *
     * @param bluejPackage the package to scan
     */
    public void scanPackage(BPackage bluejPackage) {
        try {
            String packageKey =
                    bluejPackage.getDir().getAbsolutePath();

            Set<String> classNames = new HashSet<>();

            for (BClass bluejClass : bluejPackage.getClasses()) {
                classNames.add(bluejClass.getName());

                editorRegistry.attach(packageKey, bluejClass,bluejClass.getJavaEditor());
            }

            tracker.observe(packageKey, classNames);

        } catch (ProjectNotOpenException |
                 PackageNotFoundException exception) {
            System.err.println(
                    "Could not scan package: " + exception.getMessage()
            );
        }
    }

    /**
     * Scans every package in each currently open BlueJ project.
     * A project that closes during scanning is reported and skipped.
     */
    public void scanOpenProjects() {
        for (BProject project : bluej.getOpenProjects()) {
            try {
                for (BPackage bluejPackage : project.getPackages()) {
                    scanPackage(bluejPackage);
                }
            } catch (ProjectNotOpenException exception) {
                System.err.println(
                        "Project closed during scan: "
                        + exception.getMessage()
                );
            }
        }
    }

    /**
     * Removes a package's editor listeners and class inventory state.
     * Package access failures are written to standard error.
     *
     * @param bluejPackage the package to forget
     */
    public void forgetPackage(BPackage bluejPackage) {
        try {
            String packageKey =
                    bluejPackage.getDir().getAbsolutePath();

            editorRegistry.detachPackage(packageKey);
            tracker.forget(packageKey);

        } catch (ProjectNotOpenException |
                PackageNotFoundException exception) {
            System.err.println(
                    "Could not clear package inventory: "
                    + exception.getMessage()
            );
        }
    }
}
