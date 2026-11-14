package tt.uwi.collection;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import bluej.extensions2.event.CompileEvent;
import org.junit.jupiter.api.Test;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;
import tt.uwi.telemetry.model.TelemetrySession;

import static org.junit.jupiter.api.Assertions.*;

public class BlueJCompileListenerTest {

    @Test
    void recordsCompilerErrorDetailsAndSourceFile() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        BlueJCompileListener listener =
                new BlueJCompileListener(subject);

        File sourceFile = new File("Student.java");

        CompileEvent notification = new CompileEvent(
                CompileEvent.EventType.COMPILE_ERROR_EVENT,
                new File[] { sourceFile }
        );

        notification.setErrorMessage("';' expected");
        notification.setErrorLineNumber(11);

        listener.compileError(notification);

        assertEquals(1, session.getEvents().size());

        TelemetryEvent recorded = session.getEvents().get(0);

        assertEquals(EventType.COMPILE_ERROR, recorded.getType());
        assertEquals("';' expected", recorded.getDescription());
        assertEquals(Integer.valueOf(11), recorded.getLineNumber());
        assertNull(recorded.getClassName());

        assertEquals(
                List.of(sourceFile.getAbsolutePath()),
                recorded.getSourceFiles()
        );

        assertEquals(0L, session.getTotalCompilations());
    }

    @Test
    void preservesMultipleFilesAcrossCompilationStartAndSuccess() {
        TelemetrySession session = new TelemetrySession(
                "TEST-STUDENT",
                LocalDateTime.of(2026, 11, 11, 10, 0)
        );

        TelemetrySubject subject = new TelemetrySubject();
        subject.addObserver(new TelemetryCollector(session));

        BlueJCompileListener listener =
                new BlueJCompileListener(subject);

        File firstFile = new File("Student.java");
        File secondFile = new File("Course.java");
        File[] files = { firstFile, secondFile };

        listener.compileStarted(new CompileEvent(
                CompileEvent.EventType.COMPILE_START_EVENT,
                files
        ));

        listener.compileSucceeded(new CompileEvent(
                CompileEvent.EventType.COMPILE_DONE_EVENT,
                files
        ));

        List<String> expectedFiles = List.of(
                firstFile.getAbsolutePath(),
                secondFile.getAbsolutePath()
        );

        assertEquals(2, session.getEvents().size());

        TelemetryEvent started = session.getEvents().get(0);
        TelemetryEvent succeeded = session.getEvents().get(1);

        assertEquals(EventType.COMPILE_STARTED, started.getType());
        assertEquals(EventType.COMPILE_SUCCESS, succeeded.getType());

        assertEquals(expectedFiles, started.getSourceFiles());
        assertEquals(expectedFiles, succeeded.getSourceFiles());

        assertEquals(1L, session.getTotalCompilations());
        assertEquals(1L, session.getSuccessfulCompilations());
    }
}