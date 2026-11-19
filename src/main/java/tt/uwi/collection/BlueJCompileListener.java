package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Receives BlueJ compilation events and publishes corresponding telemetry.
 * Optional callbacks keep class and method inventories synchronized after
 * compilation events.
 */
public class BlueJCompileListener implements CompileListener {

    /** Publishes telemetry events to registered observers. */
    private final TelemetrySubject subject;

    /** Callback that refreshes class inventory, or a no-op by default. */
    private final Runnable scanClasses;

    /** Callback that refreshes method inventory, or a no-op by default. */
    private final Runnable  scanMethods;

    /**
     * Constructs a compile listener with no inventory refresh callbacks.
     *
     * @param subject the subject used to publish telemetry
     */
    public BlueJCompileListener(TelemetrySubject subject) {
        this(subject, () -> {});
    }

    /**
     * Constructs a compile listener with a class-inventory refresh callback.
     *
     * @param subject the subject used to publish telemetry
     * @param scanClasses callback that refreshes the class inventory
     */
    public BlueJCompileListener(
            TelemetrySubject subject,
            Runnable scanClasses) {

        this(subject, scanClasses, () -> {});
    }

    /**
     * Constructs a compile listener with class and method refresh callbacks.
     *
     * @param subject the subject used to publish telemetry
     * @param scanClasses callback that refreshes the class inventory
     * @param scanMethods callback that refreshes the method inventory
     */
    public BlueJCompileListener(
            TelemetrySubject subject,
            Runnable scanClasses,
            Runnable scanMethods) {

        this.subject = subject;
        this.scanClasses = scanClasses;
        this.scanMethods = scanMethods;
    }

    /**
     * Extracts absolute paths for all non-null files in a compile event.
     *
     * @param event the compile event whose files are to be collected
     * @return a list of absolute source-file paths
     */
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

    /**
     * Handles a successful compilation, publishes success telemetry, and
     * refreshes the method inventory.
     *
     * @param event the successful compilation event
     */
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

    /**
     * Handles a failed compilation and publishes failure telemetry.
     *
     * @param event the failed compilation event
     */
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

    /**
     * Handles a compilation start, refreshes the class inventory, and
     * publishes start telemetry.
     *
     * @param event the compilation-start event
     */
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

    /**
     * Handles an individual compiler error and publishes its message and
     * positive line number, when available.
     *
     * @param event the compile-error event
     */
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

    /**
     * Handles a compiler warning notification.
     * Warnings are currently not published as telemetry.
     *
     * @param event the compile-warning event
     */
    @Override 
    public void compileWarning(CompileEvent event){
        
    }
}
