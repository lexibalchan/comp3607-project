package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BlueJCompileListener implements CompileListener {

    private final TelemetrySubject subject;
    private final Runnable scanClasses;
    private final Runnable  scanMethods;

    public BlueJCompileListener(TelemetrySubject subject) {
        this(subject, () -> {});
    }

    public BlueJCompileListener(
            TelemetrySubject subject,
            Runnable scanClasses) {

        this(subject, scanClasses, () -> {});
    }

    public BlueJCompileListener(
            TelemetrySubject subject,
            Runnable scanClasses,
            Runnable scanMethods) {

        this.subject = subject;
        this.scanClasses = scanClasses;
        this.scanMethods = scanMethods;
    }

    private List<String> sourceFilesOf(CompileEvent event) {
        List<String> paths = new ArrayList<>();
        File[] files = event.getFiles();

        if (files != null) {
            for (File file : files) {
                if (file != null) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }

        return paths;
    }

    @Override 
    public void compileSucceeded(CompileEvent event) {
        System.out.println(
            "Compile SUCCESS | user-generated: "
            + event.isUserGeneratedCompilation()
            + " | files: "
            + java.util.Arrays.toString(event.getFiles())
            
        );
        subject.publish(new TelemetryEvent(
                EventType.COMPILE_SUCCESS,
                LocalDateTime.now(),
                "Compilation succeeded", 
                null,
                null,
                sourceFilesOf(event)
        ));

        scanMethods.run();
    }

    @Override 
    public void compileFailed(CompileEvent event) {
        System.out.println(
            "Compile FAILURE | user-generated: "
            + event.isUserGeneratedCompilation()
            + " | files: "
            + java.util.Arrays.toString(event.getFiles())
            
        );
        subject.publish(new TelemetryEvent(
                EventType.COMPILE_FAILURE,
                LocalDateTime.now(),
                "Compilation failed", 
                null,
                null,
                sourceFilesOf(event)
        ));
    }

    @Override 
    public void compileStarted(CompileEvent event){
        scanClasses.run();
        subject.publish(new TelemetryEvent(
                EventType.COMPILE_STARTED, 
                LocalDateTime.now(), 
                "Compilation started", 
                null,
                null,
                sourceFilesOf(event)
        ));
    }

    @Override
    public void compileError(CompileEvent event) {
        int reportedLine = event.getErrorLineNumber();
        Integer lineNumber = reportedLine > 0 ? reportedLine : null;

        subject.publish(new TelemetryEvent(
                EventType.COMPILE_ERROR,
                LocalDateTime.now(),
                event.getErrorMessage(),
                null,
                lineNumber,
                sourceFilesOf(event)
        ));
    }
    
    @Override 
    public void compileWarning(CompileEvent event){
        
    }
}
