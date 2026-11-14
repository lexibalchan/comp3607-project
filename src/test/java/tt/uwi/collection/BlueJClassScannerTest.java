package tt.uwi.collection;

import java.io.File;
import java.time.LocalDateTime;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.PackageNotFoundException;

import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlueJClassScannerTest {

    @Test
    void scansOpenProjectsAndDetectsANewClass() throws Exception {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        BlueJ bluej = mock(BlueJ.class);
        BProject project = mock(BProject.class);
        BPackage bluejPackage = mock(BPackage.class);

        BClass learner = mock(BClass.class);
        BClass course = mock(BClass.class);

        JavaEditor learnerEditor = mock(JavaEditor.class);
        JavaEditor courseEditor = mock(JavaEditor.class);

        File directory = new File("test-project");
        String packageKey = directory.getAbsolutePath();

        when(bluej.getOpenProjects())
                .thenReturn(new BProject[] { project });
        when(project.getPackages())
                .thenReturn(new BPackage[] { bluejPackage });
        when(bluejPackage.getDir()).thenReturn(directory);

        when(learner.getName()).thenReturn("Learner");
        when(course.getName()).thenReturn("Course");

        when(learner.getJavaEditor()).thenReturn(learnerEditor);
        when(course.getJavaEditor()).thenReturn(courseEditor);

        when(bluejPackage.getClasses())
                .thenReturn(
                        new BClass[] { learner },
                        new BClass[] { learner, course }
                );

        EditorListenerRegistry registry =
                mock(EditorListenerRegistry.class);

        ClassInventoryTracker tracker =
                new ClassInventoryTracker(subject);

        BlueJClassScanner scanner =
                new BlueJClassScanner(bluej, tracker, registry);

        scanner.scanOpenProjects();
        assertTrue(session.getEvents().isEmpty());

        scanner.scanOpenProjects();

        assertEquals(1, session.getEvents().size());
        assertEquals(
                EventType.CLASS_CREATED,
                session.getEvents().get(0).getType()
        );
        assertEquals(
                "Course",
                session.getEvents().get(0).getClassName()
        );

        verify(registry, times(2))
                .attach(packageKey, learner, learnerEditor);
        verify(registry)
                .attach(packageKey, course, courseEditor);
    }

    @Test
    void failedScanPreservesThePreviousInventory() throws Exception {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        BlueJ bluej = mock(BlueJ.class);
        BPackage bluejPackage = mock(BPackage.class);
        BClass learner = mock(BClass.class);

        when(bluejPackage.getDir()).thenReturn(new File("test-project"));
        when(learner.getName()).thenReturn("Learner");

        when(bluejPackage.getClasses())
                .thenReturn(new BClass[] { learner })
                .thenThrow(mock(PackageNotFoundException.class))
                .thenReturn(new BClass[] { learner });

        EditorListenerRegistry registry =
                mock(EditorListenerRegistry.class);

        ClassInventoryTracker tracker =
                new ClassInventoryTracker(subject);

        BlueJClassScanner scanner =
                new BlueJClassScanner(bluej, tracker, registry);

        scanner.scanPackage(bluejPackage);
        scanner.scanPackage(bluejPackage);
        scanner.scanPackage(bluejPackage);

        assertTrue(session.getEvents().isEmpty());
    }
}