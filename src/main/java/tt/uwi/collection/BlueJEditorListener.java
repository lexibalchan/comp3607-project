package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.DocumentListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

/**
 * Receives text-edit notifications from a BlueJ Java editor and publishes
 * editor-activity telemetry for the associated class.
 */

public class BlueJEditorListener implements DocumentListener {

    /** Publishes editor activity to registered telemetry observers. */
    private final TelemetrySubject subject;

    /** The BlueJ class associated with the edited document. */
    private final BClass bluejClass;

    /**
     * Constructs an editor listener for a specific BlueJ class.
     *
     * @param subject the subject used to publish telemetry
     * @param bluejClass the class associated with the edited document
     */
    public BlueJEditorListener(
            TelemetrySubject subject, BClass bluejClass) {
        this.subject = subject;
        this.bluejClass = bluejClass;
    }

    /**
     * Handles a text replacement in the associated BlueJ editor document.
     * Publishes one editor-activity event carrying the current class name.
     * The replacement position and text/line counts are received for callback
     * compatibility but are not included in the telemetry event.
     *
     * @param position the replacement position in the document
     * @param removedText the text removed by the replacement
     * @param insertedText the text inserted by the replacement
     * @param linesRemoved the number of lines removed
     * @param linesAdded the number of lines added
     */
    @Override
    public void textReplaced(
            int position,
            String removedText,
            String insertedText,
            int linesRemoved,
            int linesAdded) {

        subject.publish(new TelemetryEvent(
                EventType.EDITOR_ACTIVITY,
                LocalDateTime.now(),
                "Editor document changed",
                bluejClass.getName()
        ));
    }
}
