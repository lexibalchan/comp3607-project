package tt.uwi.collection;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.JavaEditor;

/**
 * Tracks document-listener registrations for BlueJ classes, grouped by
 * package key, to prevent duplicate attachment and support package cleanup.
 */
public class EditorListenerRegistry {

    /** Publishes telemetry from registered editor listeners. */
    private final TelemetrySubject subject;

    /** Package-keyed registrations using identity for BlueJ class instances. */
    private final Map<String, Map<BClass, Registration>> packages =
            new HashMap<>();

    /**
     * Constructs an editor listener registry.
     *
     * @param subject the subject used by created editor listeners
     */
    public EditorListenerRegistry(TelemetrySubject subject) {
        this.subject = subject;
    }

    /**
     * Attaches one document listener to a class's Java editor.
     * A null editor or an already registered class is ignored.
     *
     * @param packageKey the key identifying the package
     * @param bluejClass the BlueJ class represented by the listener
     * @param editor the editor document listener container
     */
    public void attach(
            String packageKey, BClass bluejClass, JavaEditor editor) {

        if (editor == null) {
            return;
        }

        Map<BClass, Registration> registrations =
                packages.computeIfAbsent(
                        packageKey,
                        key -> new IdentityHashMap<>()
                );

        if (registrations.containsKey(bluejClass)) {
            return;
        }

        BlueJEditorListener listener =
                new BlueJEditorListener(subject, bluejClass);

        editor.addDocumentListener(listener);

        registrations.put(
                bluejClass,
                new Registration(editor, listener)
        );
    }

    /**
     * Removes and unregisters all editor listeners associated with a package.
     * Detaching an unknown or already detached package has no effect.
     *
     * @param packageKey the key identifying the package to detach
     */
    public void detachPackage(String packageKey) {
        Map<BClass, Registration> registrations =
                packages.remove(packageKey);

        if (registrations == null) {
            return;
        }

        for (Registration registration : registrations.values()) {
            registration.editor().removeDocumentListener(
                    registration.listener()
            );
        }
    }

    /**
     * Removes and unregisters every editor listener held by this registry.
     */
    public void detachAll() {
        for (String packageKey :
                packages.keySet().toArray(new String[0])) {
            detachPackage(packageKey);
        }
    }

    /**
     * Stores the editor and listener pair for a registered BlueJ class.
     *
     * @param editor the Java editor from which the listener can be removed
     * @param listener the attached document listener
     */
    private record Registration(
            JavaEditor editor,
            BlueJEditorListener listener) {
    }
}
