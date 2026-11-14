package tt.uwi.collection;

import java.util.HashSet;
import java.util.Set;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;


public class BlueJClassScanner {

    private final BlueJ bluej;
    private final ClassInventoryTracker tracker;
    private final EditorListenerRegistry editorRegistry;

    public BlueJClassScanner(
            BlueJ bluej, ClassInventoryTracker tracker, EditorListenerRegistry editorRegistry) {
        this.bluej = bluej;
        this.tracker = tracker;
        this.editorRegistry = editorRegistry;
    }

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