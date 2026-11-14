package tt.uwi.collection;

import java.time.LocalDateTime;

import bluej.extensions2.BClass;
import bluej.extensions2.editor.DocumentListener;

import tt.uwi.telemetry.model.EventType;
import tt.uwi.telemetry.model.TelemetryEvent;

public class BlueJEditorListener implements DocumentListener {

    private final TelemetrySubject subject;
    private final BClass bluejClass;

    public BlueJEditorListener(
            TelemetrySubject subject, BClass bluejClass) {
        this.subject = subject;
        this.bluejClass = bluejClass;
    }

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