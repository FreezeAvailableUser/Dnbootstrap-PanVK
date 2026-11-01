package git.artdeell.dnbootstrap.input.editor;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.input.ControlButton;
import git.artdeell.dnbootstrap.input.model.ControlButtonData;

public class ButtonEditorDialog extends InputConfigurationEditorDialog {
    private ListPopupWindow listPopupWindow;
    private final TextView[] keycodeViews = new TextView[2];
    private EditText labelEdit;
    //TODO: move visibility configuration into an abstract dialog class (or into LayoutEditorDialog?)
    private CheckBox showInGameCheckbox;
    private CheckBox showInMenuCheckbox;
    private Button backgroundColorButton;
    private ImageView backgroundAssetSelector;
    private int editingKeycode;
    private final int[] controlButtonKeycodes = new int[2];
    private int selectedBackgroundColor;
    private String selectedBackgroundAsset;
    private String[] backgroundAssets;
    private static final String ASSET_PREFIX = "btn_icon_";

    private static final int[] BACKGROUND_COLORS = {
        0x00000000, // None
        0x80000000, // Transparent
        0x804CAF50, // Green
        0x802196F3, // Blue
        0x80FF9800, // Orange
        0x80F44336  // Red
    };

    public ButtonEditorDialog() {
        super(R.layout.dialog_button_setup);
        backgroundAssets = findDrawableNamesByPrefix(ASSET_PREFIX);
    }

    @Override
    protected void inflate(Dialog dialog) {
        super.inflate(dialog);
        View anchorStub = dialog.findViewById(R.id.editor_keycode_select_anchor);
        keycodeViews[0] = dialog.findViewById(R.id.editor_keycode_select_1);
        keycodeViews[1] = dialog.findViewById(R.id.editor_keycode_select_2);
        labelEdit = dialog.findViewById(R.id.editor_label_text);
        showInGameCheckbox = dialog.findViewById(R.id.editor_show_in_game);
        showInMenuCheckbox = dialog.findViewById(R.id.editor_show_in_menu);
        backgroundColorButton = dialog.findViewById(R.id.editor_background_color_button);
        backgroundAssetSelector = dialog.findViewById(R.id.editor_background_asset_selector);
        for(int i = 0; i < keycodeViews.length; i++) {
            int keycodeIndex = i;
            keycodeViews[i].setOnClickListener((v)->openKeycodeSpinner(keycodeIndex));
        }

        listPopupWindow = new ListPopupWindow(dialog.getContext());
        listPopupWindow.setAdapter(createSpinner());
        listPopupWindow.setWidth(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setAnchorView(anchorStub);
        listPopupWindow.setOnItemClickListener(new KeyCodeSelectedListener());

        backgroundColorButton.setOnClickListener(v -> openColorPicker());
        backgroundAssetSelector.setOnClickListener(v -> openAssetPickerDialog());
    }

    @Override
    protected void loadSettings() {
        super.loadSettings();
        ControlButtonData controlButtonData = ((ControlButton) getEditTarget()).controlButtonData;
        for(int i = 0; i < keycodeViews.length; i++) {
            controlButtonKeycodes[i] = controlButtonData.keyCodes[i];
            DisplayKeyCode displayKeyCode = DisplayKeyCode.findEntryByCode(controlButtonKeycodes[i]);
            keycodeViews[i].setText(displayKeyCode.name());
        }
        labelEdit.setText(controlButtonData.label);
        showInGameCheckbox.setChecked(controlButtonData.showInGame);
        showInMenuCheckbox.setChecked(controlButtonData.showInMenu);
        selectedBackgroundColor = controlButtonData.backgroundColor;
        selectedBackgroundAsset = controlButtonData.backgroundAssetId;
        updateColorButtonAppearance();
        updateAssetSelectorAppearance();
    }

    @Override
    protected void saveSettings() {
        super.saveSettings();
        ControlButton editTarget = (ControlButton) getEditTarget();
        ControlButtonData controlButtonData = editTarget.controlButtonData;
        System.arraycopy(controlButtonKeycodes, 0, controlButtonData.keyCodes, 0, controlButtonKeycodes.length);
        controlButtonData.label = labelEdit.getText().toString();
        editTarget.setText(controlButtonData.label);
        controlButtonData.showInGame = showInGameCheckbox.isChecked();
        controlButtonData.showInMenu = showInMenuCheckbox.isChecked();
        controlButtonData.backgroundColor = selectedBackgroundColor;
        controlButtonData.backgroundAssetId = selectedBackgroundAsset;
        editTarget.applyBackground();
    }

    private void openKeycodeSpinner(int index) {
        listPopupWindow.show();
        editingKeycode = -1;
        listPopupWindow.setSelection(DisplayKeyCode.findIndexByCode(controlButtonKeycodes[index]));
        editingKeycode = index;
    }

    private void openColorPicker() {
        Context context = backgroundColorButton.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Color");

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 5));

        List<GridItem> items = new ArrayList<>();
        for (int color : BACKGROUND_COLORS) {
            items.add(new ColorItem(color));
        }

        GridPickerAdapter adapter = new GridPickerAdapter(
                context,
                items,
                selectedBackgroundColor,
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

    private void openAssetPickerDialog() {
        Context context = backgroundAssetSelector.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Icon");

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 5));

        List<GridItem> items = new ArrayList<>();
        if (backgroundAssets != null) {
            for (String asset : backgroundAssets) {
                items.add(new IconItem(asset));
            }
        }

        GridPickerAdapter adapter = new GridPickerAdapter(
                context,
                items,
                selectedBackgroundAsset,
                (value) -> {
                    selectedBackgroundAsset = (String) value;
                    updateAssetSelectorAppearance();
                }
        );
        recyclerView.setAdapter(adapter);

        builder.setView(recyclerView);
        AlertDialog dialog = builder.create();
        adapter.setDialog(dialog);
        dialog.show();
    }

    private void updateAssetSelectorAppearance() {
        String assetName = selectedBackgroundAsset;
        if("None".equals(assetName) || assetName == null || assetName.isEmpty()) {
            backgroundAssetSelector.setImageDrawable(null);
            backgroundAssetSelector.setImageResource(R.drawable.select_empty_material);
        } else {
            int drawableId = backgroundAssetSelector.getContext().getResources().getIdentifier(
                    assetName,
                    "drawable",
                    backgroundAssetSelector.getContext().getPackageName()
            );
            if(drawableId != 0) {
                backgroundAssetSelector.setImageResource(drawableId);
            } else {
                backgroundAssetSelector.setImageDrawable(null);
            }
        }
    }

    private ListAdapter createSpinner() {
        return new ArrayAdapter<>(getEditTarget().getContext(), R.layout.key_list_item, DisplayKeyCode.values());
    }

    private String[] findDrawableNamesByPrefix(String prefix) {
        List<String> names = new ArrayList<>();

        try {
            for (Field field : R.drawable.class.getFields()) {
                String name = field.getName();
                if (name.startsWith(prefix)) {
                    names.add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return names.toArray(new String[0]);
    }

    private class KeyCodeSelectedListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            if(editingKeycode == -1) return;
            DisplayKeyCode keyCode = DisplayKeyCode.values()[i];
            keycodeViews[editingKeycode].setText(keyCode.name());
            controlButtonKeycodes[editingKeycode] = keyCode.code;
            listPopupWindow.dismiss();
        }
    }
}
