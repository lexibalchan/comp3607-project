package tt.uwi.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bluej.extensions2.BClass;
import bluej.extensions2.BMethod;
import bluej.extensions2.BPackage;
import bluej.extensions2.BlueJ;

import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlueJMethodScannerTest {

    @Test
    void preservesBaselineUntilClassIsCompiled() throws Exception {
        List<TelemetryEvent> events = new ArrayList<>();
        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(events::add);

        MethodInventoryTracker tracker =
                new MethodInventoryTracker(subject);

        BlueJMethodScanner scanner =
                new BlueJMethodScanner(mock(BlueJ.class), tracker);

        BPackage bluejPackage = mock(BPackage.class);
        BClass student = mock(BClass.class);
        BMethod calculate = mock(BMethod.class);

        when(bluejPackage.getDir()).thenReturn(new File("test-project"));
        when(bluejPackage.getClasses()).thenReturn(new BClass[]{student});

        when(student.getName()).thenReturn("Student");
        when(student.isCompiled()).thenReturn(true);
        when(student.getDeclaredMethods())
                .thenReturn(new BMethod[]{calculate});

        when(calculate.getName()).thenReturn("calculate");
        when(calculate.getParameterTypes())
                .thenReturn(new Class<?>[]{int.class});
        doReturn(double.class).when(calculate).getReturnType();

        scanner.scanPackage(bluejPackage);

        assertTrue(events.isEmpty());

        when(student.isCompiled()).thenReturn(false);
        when(student.getDeclaredMethods()).thenReturn(new BMethod[0]);

        scanner.scanPackage(bluejPackage);

        assertTrue(events.isEmpty());
        verify(student, times(1)).getDeclaredMethods();

        when(student.isCompiled()).thenReturn(true);

        scanner.scanPackage(bluejPackage);

        assertEquals(1, events.size());
        assertEquals(EventType.METHOD_REMOVED, events.get(0).getType());
        assertTrue(events.get(0).getDescription()
                .contains("calculate(int): double"));
    }
}