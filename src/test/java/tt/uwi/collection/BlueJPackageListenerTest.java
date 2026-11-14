package tt.uwi.collection;

import java.io.File;
import java.time.LocalDateTime;

import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.event.PackageEvent;

import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlueJPackageListenerTest {

    @Test
    void recordsPackageLifecycleAndScansBeforeCleanup() throws Exception {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        BProject project = mock(BProject.class);
        BPackage bluejPackage = mock(BPackage.class);
        PackageEvent notification = mock(PackageEvent.class);

        File projectDirectory = new File("test-project");

        when(notification.getPackage()).thenReturn(bluejPackage);
        when(bluejPackage.getProject()).thenReturn(project);
        when(bluejPackage.getName()).thenReturn("");
        when(project.getDir()).thenReturn(projectDirectory);

        BlueJClassScanner scanner = mock(BlueJClassScanner.class);

        BlueJPackageListener listener =
                new BlueJPackageListener(subject, scanner);

        listener.packageOpened(notification);
        listener.packageClosing(notification);

        assertEquals(2, session.getEvents().size());

        var opened = session.getEvents().get(0);
        var closing = session.getEvents().get(1);

        assertEquals(EventType.PACKAGE_OPENED, opened.getType());
        assertEquals(EventType.PACKAGE_CLOSING, closing.getType());

        assertTrue(opened.getDescription().contains("(default package)"));
        assertTrue(opened.getDescription().contains(
                projectDirectory.getAbsolutePath()
        ));
        assertEquals(opened.getDescription(), closing.getDescription());

        var order = inOrder(scanner);
        order.verify(scanner, times(2)).scanPackage(bluejPackage);
        order.verify(scanner).forgetPackage(bluejPackage);
        order.verifyNoMoreInteractions();
    }
}