package tt.uwi.collection;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.JavaEditor;

public class EditorListenerRegistry {

    private final TelemetrySubject subject;

    private final Map<String, Map<BClass, Registration>> packages =
            new HashMap<>();

    public EditorListenerRegistry(TelemetrySubject subject) {
        this.subject = subject;
    }

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

    public void detachAll() {
        for (String packageKey :
                packages.keySet().toArray(new String[0])) {
            detachPackage(packageKey);
        }
    }

    private record Registration(
            JavaEditor editor,
            BlueJEditorListener listener) {
    }
}