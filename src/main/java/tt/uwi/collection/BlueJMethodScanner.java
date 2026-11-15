package tt.uwi.collection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BlueJ;
import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;

public class BlueJMethodScanner {

    private final BlueJ bluej;
    private final MethodInventoryTracker tracker;

    public BlueJMethodScanner(
            BlueJ bluej,
            MethodInventoryTracker tracker) {

        this.bluej = bluej;
        this.tracker = tracker;
    }

    public void scanOpenProjects() {
        for (var project : bluej.getOpenProjects()) {
            try {
                for (BPackage bluejPackage : project.getPackages()) {
                    scanPackage(bluejPackage);
                }
            } catch (ProjectNotOpenException exception) {
                System.err.println(
                        "Project closed during method scan: "
                                + exception.getMessage()
                );
            }
        }
    }

    public void scanPackage(BPackage bluejPackage) {
        try {
            String directory =
                    bluejPackage.getDir().getAbsolutePath();

            BClass[] classes = bluejPackage.getClasses();

            Set<String> currentClassNames = new HashSet<>();

            for (BClass bluejClass : classes) {
                currentClassNames.add(bluejClass.getName());
            }

            tracker.retainClasses(directory, currentClassNames);

            for (BClass bluejClass : classes) {
                scanClass(directory, bluejClass);
            }
        } catch (ProjectNotOpenException |
                 PackageNotFoundException exception) {
            System.err.println(
                    "Could not scan package methods: "
                            + exception.getMessage()
            );
        }
    }

    private void scanClass(String directory, BClass bluejClass) {
        try {
            if (!bluejClass.isCompiled()) {
                return;
            }

            Set<String> signatures = new HashSet<>();

            for (var method : bluejClass.getDeclaredMethods()) {
                String parameters = Arrays.stream(method.getParameterTypes())
                        .map(Class::getTypeName)
                        .collect(Collectors.joining(", "));

                String signature = method.getName()
                        + "(" + parameters + ")"
                        + ": " + method.getReturnType().getTypeName();

                signatures.add(signature);
            }

            tracker.observe(directory, bluejClass.getName(), signatures);

        } catch (ProjectNotOpenException |
                 PackageNotFoundException |
                 bluej.extensions2.ClassNotFoundException exception) {
            System.err.println(
                    "Could not inspect methods for "
                            + bluejClass.getName() + ": "
                            + exception.getMessage()
            );
        }
    }
}