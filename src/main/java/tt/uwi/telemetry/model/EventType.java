package tt.uwi.telemetry.model;

/**
 * Identifies the category of a telemetry event generated during a BlueJ session.
 */
public enum EventType {
    /** A BlueJ project was opened. */
    PROJECT_OPENED,

    /** A class was created. */
    CLASS_CREATED,

    /** A class was modified. */
    CLASS_MODIFIED,

    /** A class was renamed. */
    CLASS_RENAMED,

    /** A class was deleted. */
    CLASS_DELETED,

    /** A class's compiled or error state changed. */
    CLASS_STATE_CHANGED,

    /** Text activity occurred in a BlueJ editor. */
    EDITOR_ACTIVITY,

    /** A compilation started. */
    COMPILE_STARTED,

    /** A compilation completed successfully. */
    COMPILE_SUCCESS,

    /** A compilation failed. */
    COMPILE_FAILURE,

    /** A compiler error was reported. */
    COMPILE_ERROR,

    /** A program or method invocation completed. */
    PROGRAM_RUN,

    /** A program invocation ended with a runtime exception. */
    RUNTIME_EXCEPTION,

    /** A method was added to a class. */
    METHOD_ADDED,

    /** A method was removed from a class. */
    METHOD_REMOVED,

    /** A BlueJ package was opened. */
    PACKAGE_OPENED,

    /** A BlueJ package began closing. */
    PACKAGE_CLOSING
}
