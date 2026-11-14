package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.DocumentListener;
import bluej.extensions2.editor.JavaEditor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class EditorListenerRegistryTest {

    @Test
    void attachesOnceRecordsEditAndRemovesSameListener() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        BClass bluejClass = mock(BClass.class);
        JavaEditor editor = mock(JavaEditor.class);

        when(bluejClass.getName()).thenReturn("Learner");

        EditorListenerRegistry registry =
                new EditorListenerRegistry(subject);

        registry.attach("test-project/default", bluejClass, editor);
        registry.attach("test-project/default", bluejClass, editor);

        ArgumentCaptor<DocumentListener> captured =
                ArgumentCaptor.forClass(DocumentListener.class);

        verify(editor, times(1))
                .addDocumentListener(captured.capture());

        DocumentListener listener = captured.getValue();

        listener.textReplaced(0, "", "x", 0, 0);

        assertEquals(1, session.getEvents().size());
        assertEquals(
                EventType.EDITOR_ACTIVITY,
                session.getEvents().get(0).getType()
        );
        assertEquals(
                "Learner",
                session.getEvents().get(0).getClassName()
        );

        registry.detachPackage("test-project/default");
        registry.detachPackage("test-project/default");

        verify(editor, times(1))
                .removeDocumentListener(same(listener));
    }

    @Test
    void closingOnePackageLeavesOtherPackageRegistered() {
        TelemetrySubject subject = new TelemetrySubject();

        BClass firstClass = mock(BClass.class);
        BClass secondClass = mock(BClass.class);

        JavaEditor firstEditor = mock(JavaEditor.class);
        JavaEditor secondEditor = mock(JavaEditor.class);

        EditorListenerRegistry registry =
                new EditorListenerRegistry(subject);

        registry.attach("project-one/default", firstClass, firstEditor);
        registry.attach("project-two/default", secondClass, secondEditor);

        registry.detachPackage("project-one/default");

        verify(firstEditor).removeDocumentListener(
                any(DocumentListener.class)
        );
        verify(secondEditor, never()).removeDocumentListener(
                any(DocumentListener.class)
        );

        registry.detachAll();

        verify(firstEditor, times(1)).removeDocumentListener(
                any(DocumentListener.class)
        );
        verify(secondEditor, times(1)).removeDocumentListener(
                any(DocumentListener.class)
        );
    }
}