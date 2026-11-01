package git.artdeell.dnbootstrap.input.editor;

import android.app.Dialog;
import android.widget.CheckBox;

import androidx.annotation.CallSuper;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.input.LayoutTouchConsumer;
import git.artdeell.dnbootstrap.input.model.InputConfiguration;

public abstract class InputConfigurationEditorDialog extends LayoutEditorDialog {
    private CheckBox stickyCheck;
    private CheckBox movesCursorCheck;
    public InputConfigurationEditorDialog(int viewId) {
        super(viewId);
    }

    @CallSuper
    @Override
    protected void inflate(Dialog dialog) {
        super.inflate(dialog);
        stickyCheck = dialog.findViewById(R.id.editor_sticky);
        movesCursorCheck = dialog.findViewById(R.id.editor_moves_cursor);
    }

    @CallSuper
    @Override
    protected void loadSettings() {
        InputConfiguration inputConfiguration = ((LayoutTouchConsumer)getEditTarget()).getInputConfiguration();
        stickyCheck.setChecked(inputConfiguration.sticky);
        movesCursorCheck.setChecked(inputConfiguration.movesCursor);
    }

    @CallSuper
    @Override
    protected void saveSettings() {
        InputConfiguration inputConfiguration = ((LayoutTouchConsumer)getEditTarget()).getInputConfiguration();
        inputConfiguration.sticky = stickyCheck.isChecked();
        inputConfiguration.movesCursor = movesCursorCheck.isChecked();
    }
}
