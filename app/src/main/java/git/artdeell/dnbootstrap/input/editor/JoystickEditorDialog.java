package git.artdeell.dnbootstrap.input.editor;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.input.Joystick;
import git.artdeell.dnbootstrap.input.model.JoystickData;

public class JoystickEditorDialog extends InputConfigurationEditorDialog {
    private CheckBox showInGameCheckbox;
    private CheckBox showInMenuCheckbox;
    private CheckBox autoCenterCheckbox;
    private Button backgroundColorButton;
    private int selectedBackgroundColor;
    private static final int[] BACKGROUND_COLORS = {
            0x00000000, // None
            0x80000000, // Transparent
            0x804CAF50, // Green
            0x802196F3, // Blue
            0x80FF9800, // Orange
            0x80F44336  // Red
    };

    public JoystickEditorDialog() {
        super(R.layout.dialog_joystick_setup);
    }

    @CallSuper
    @Override
    protected void inflate(Dialog dialog) {
        super.inflate(dialog);
        showInGameCheckbox = dialog.findViewById(R.id.editor_joystick_show_in_game);
        showInMenuCheckbox = dialog.findViewById(R.id.editor_joystick_show_in_menu);
        autoCenterCheckbox = dialog.findViewById(R.id.editor_joystick_auto_center);
        backgroundColorButton = dialog.findViewById(R.id.editor_background_color_button);
        backgroundColorButton.setOnClickListener(v -> openColorPicker());
    }

    @CallSuper
    @Override
    protected void loadSettings() {
        super.loadSettings();
        Joystick joystick = (Joystick) getEditTarget();
        JoystickData joystickData = joystick.joystickData;
        showInGameCheckbox.setChecked(joystickData.showInGame);
        showInMenuCheckbox.setChecked(joystickData.showInMenu);
        autoCenterCheckbox.setChecked(joystickData.autoCenter);
        selectedBackgroundColor = joystickData.backgroundColor;
        updateColorButtonAppearance();
    }

    @CallSuper
    @Override
    protected void saveSettings() {
        super.saveSettings();
        Joystick joystick = (Joystick) getEditTarget();
        JoystickData joystickData = joystick.joystickData;
        joystickData.showInGame = showInGameCheckbox.isChecked();
        joystickData.showInMenu = showInMenuCheckbox.isChecked();
        joystickData.autoCenter = autoCenterCheckbox.isChecked();
        joystickData.backgroundColor = selectedBackgroundColor;
        joystick.applyStyling();
    }

    private void openColorPicker() {
        Context context = backgroundColorButton.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Color");

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 5)); // 5 columns for colors

        // Convert int[] to List<GridItem>
        List<GridItem> items = new ArrayList<>();
        for (int color : BACKGROUND_COLORS) {
            items.add(new ColorItem(color));
        }

        GridPickerAdapter adapter = new GridPickerAdapter(
                context,
                items,
                selectedBackgroundColor, // Pass current selection
                (value) -> {
                    selectedBackgroundColor = (Integer) value;
                    updateColorButtonAppearance();
                }
        );
        recyclerView.setAdapter(adapter);

        builder.setView(recyclerView);
        AlertDialog dialog = builder.create();
        adapter.setDialog(dialog);
        dialog.show();
    }

    private void updateColorButtonAppearance() {
        if (selectedBackgroundColor == 0x00000000) {
            backgroundColorButton.setBackgroundResource(R.drawable.select_empty_material);
            return;
        }
        backgroundColorButton.setBackgroundColor(selectedBackgroundColor);
    }
}
