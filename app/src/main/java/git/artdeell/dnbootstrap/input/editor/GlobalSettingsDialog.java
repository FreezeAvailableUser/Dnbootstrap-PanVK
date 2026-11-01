package git.artdeell.dnbootstrap.input.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.input.ControlButton;
import git.artdeell.dnbootstrap.input.Joystick;
import git.artdeell.dnbootstrap.input.model.ControlButtonData;
import git.artdeell.dnbootstrap.input.model.JoystickData;

public class GlobalSettingsDialog extends PropertyDialog implements SeekBar.OnSeekBarChangeListener {
    private final ControlEditorLayout parent;
    private SeekBar gridPitchSeek;
    private CheckBox autoAlignCheck;
    private CheckBox cursorBehaviorCheck;
    private TextView gridPitchLabel;
    private float oldGridPitch;
    public GlobalSettingsDialog(ControlEditorLayout parent) {
        super(R.layout.dialog_global_settings, R.string.grid_layout_global_edit_title);
        this.parent = parent;
    }

    public void show(Context context) {
        createAndShow(context);
        gridPitchSeek.setProgress((int) (oldGridPitch = parent.gridPitchDp));
        autoAlignCheck.setChecked(parent.autoAlign);
        cursorBehaviorCheck.setChecked(parent.cursorToTouch);
    }

    @Override
    protected void inflate(Dialog dialog) {
        autoAlignCheck = dialog.findViewById(R.id.editor_global_auto_align);
        cursorBehaviorCheck = dialog.findViewById(R.id.editor_global_cursor_behavior);
        gridPitchSeek = dialog.findViewById(R.id.editor_global_pitch_seek);
        gridPitchLabel = dialog.findViewById(R.id.editor_global_pitch_view);
        gridPitchSeek.setOnSeekBarChangeListener(this);
        dialog.findViewById(R.id.editor_global_add_button).setOnClickListener(v->{
            parent.addView(new ControlButton(parent.getContext(), ControlButtonData.createDefault()));
            hide();
        });
        dialog.findViewById(R.id.editor_global_add_joystick).setOnClickListener(v->{
            parent.addView(new Joystick(parent.getContext(), JoystickData.createDefault()));
            hide();
        });
        setupAlertDialogButtons((AlertDialog) dialog);
    }

    private void setupAlertDialogButtons(AlertDialog alertDialog) {
        TextView neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        neutralButton.setText(R.string.grid_layout_global_save);
        neutralButton.setVisibility(View.VISIBLE);
        neutralButton.setOnClickListener(v->{
            if(parent.saveAsync()) hide();
        });

        if(parent.layoutEditorHost != null) {
            TextView negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setText(R.string.grid_layout_global_exit);
            negativeButton.setVisibility(View.VISIBLE);
            negativeButton.setOnClickListener(v->{
                parent.layoutEditorHost.exitLayoutEditor();
                hide();
            });
        }
    }

    @Override
    protected void onExitWithSave() {
        parent.autoAlign = autoAlignCheck.isChecked();
        parent.cursorToTouch = cursorBehaviorCheck.isChecked();
    }

    @Override
    protected void onExitDiscard() {
        parent.setGridPitch(oldGridPitch);
        parent.requestLayout();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        gridPitchLabel.setText(String.format(Locale.ENGLISH, "%d dp", i));
        parent.setGridPitch(i);
        parent.requestLayout();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
