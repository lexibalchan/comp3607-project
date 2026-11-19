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

/**
 * Scans BlueJ projects, packages, and compiled classes to track declared
 * method signatures in the method inventory.
 */

public class BlueJMethodScanner {

    /** The BlueJ environment used to access open projects. */
    private final BlueJ bluej;

    /** Tracks method signatures and publishes detected additions or removals. */
    private final MethodInventoryTracker tracker;

    /**
     * Constructs a method scanner.
     *
     * @param bluej the BlueJ environment
     * @param tracker the method inventory tracker
     */
    public BlueJMethodScanner(
            BlueJ bluej,
            MethodInventoryTracker tracker) {

        this.bluej = bluej;
        this.tracker = tracker;
    }

    /**
     * Scans every package in each currently open BlueJ project.
     * A project that closes during scanning is reported and skipped.
     */
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

    /**
     * Scans all classes in a package and removes tracked method inventory for
     * classes that are no longer in the package.
     * Package access failures are written to standard error.
     *
     * @param bluejPackage the package to scan
     */
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

    /**
     * Scans declared methods on one compiled class.
     * Uncompiled classes are skipped. Method signatures include parameter type
     * names and the return type name.
     *
     * @param directory the absolute package-directory key
     * @param bluejClass the class whose declared methods are inspected
     */
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
