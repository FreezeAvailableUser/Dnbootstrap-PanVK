package git.artdeell.dnbootstrap.input.editor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.CallSuper;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.input.LoadableButtonLayout;

public abstract class LayoutEditorDialog extends PropertyDialog {
    private LayoutEditable targetView;
    private EditText widthEdit;
    private EditText heightEdit;
    private EditText horizOffsetEdit;
    private EditText vertOffsetEdit;
    private Spinner vertAlignSpinner;
    private Spinner horizAlignSpinner;

    public LayoutEditorDialog(int viewId) {
        super(viewId, R.string.grid_layout_edit_title);
    }

    @CallSuper
    @Override
    protected void inflate(Dialog dialog) {
        widthEdit = dialog.findViewById(R.id.editor_size_width);
        heightEdit = dialog.findViewById(R.id.editor_size_height);
        horizOffsetEdit = dialog.findViewById(R.id.editor_horiz_pos_text);
        horizAlignSpinner = dialog.findViewById(R.id.editor_horiz_pos_align);
        vertOffsetEdit = dialog.findViewById(R.id.editor_vert_pos_text);
        vertAlignSpinner = dialog.findViewById(R.id.editor_vert_pos_align);
        setupAlertDialogButtons((AlertDialog) dialog);
    }

    private void setupAlertDialogButtons(AlertDialog alertDialog) {
        TextView neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        neutralButton.setText(R.string.grid_layout_edit_clone);
        neutralButton.setVisibility(View.VISIBLE);
        neutralButton.setOnClickListener(v->{
            applyLayoutSettings();
            saveSettings();
            ViewGroup parent = (ViewGroup) targetView.getParent();
            parent.addView(targetView.fullClone());
        });
        TextView negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setText(R.string.grid_layout_edit_delete);
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setOnClickListener(v->{
            ViewGroup parent = (ViewGroup) targetView.getParent();
            parent.removeView((View) targetView);
            alertDialog.dismiss();
        });
    }

    private int getHorizontalAlignment() {
        switch (horizAlignSpinner.getSelectedItemPosition()) {
            case 0: return LoadableButtonLayout.LayoutParams.ALIGNMENT_LEFT;
            case 1: return LoadableButtonLayout.LayoutParams.ALIGNMENT_RIGHT;
            case 2: return LoadableButtonLayout.LayoutParams.ALIGNMENT_CENTER;
            default:
                throw new RuntimeException("Unknown alignment selection");
        }
    }

    private int getVerticalAlignment() {
        switch (vertAlignSpinner.getSelectedItemPosition()) {
            case 0: return LoadableButtonLayout.LayoutParams.ALIGNMENT_TOP;
            case 1: return LoadableButtonLayout.LayoutParams.ALIGNMENT_BOTTOM;
            case 2: return LoadableButtonLayout.LayoutParams.ALIGNMENT_CENTER;
            default:
                throw new RuntimeException("Unknown alignment selection");
        }
    }

    private void setHorizontalAlignment(int alignment) {
        int spinnerIdx;
        switch (alignment) {
            case LoadableButtonLayout.LayoutParams.ALIGNMENT_LEFT: spinnerIdx = 0; break;
            case LoadableButtonLayout.LayoutParams.ALIGNMENT_RIGHT: spinnerIdx = 1; break;
            case LoadableButtonLayout.LayoutParams.ALIGNMENT_CENTER: spinnerIdx = 2; break;
            default:
                throw new RuntimeException("Unknown alignment type: "+alignment);
        }
        horizAlignSpinner.setSelection(spinnerIdx);
    }

    private void setVerticalAlignment(int alignment) {
        int spinnerIdx;
        switch (alignment) {
            case LoadableButtonLayout.LayoutParams.ALIGNMENT_TOP: spinnerIdx = 0; break;
            case LoadableButtonLayout.LayoutParams.ALIGNMENT_BOTTOM: spinnerIdx = 1; break;
            case LoadableButtonLayout.LayoutParams.ALIGNMENT_CENTER: spinnerIdx = 2; break;
            default:
                throw new RuntimeException("Unknown alignment type: "+alignment);
        }
        vertAlignSpinner.setSelection(spinnerIdx);
    }

    @SuppressLint("SetTextI18n")
    private void loadLayoutSettings(LoadableButtonLayout.LayoutParams params) {
        widthEdit.setText(Integer.toString(params.width));
        heightEdit.setText(Integer.toString(params.height));
        horizOffsetEdit.setText(Integer.toString(params.offsetHorizontal));
        vertOffsetEdit.setText(Integer.toString(params.offsetVertical));
        setVerticalAlignment(params.alignmentVertical);
        setHorizontalAlignment(params.alignmentHorizontal);
    }

    protected void applyLayoutSettings() {
        LoadableButtonLayout.LayoutParams layoutParams = (LoadableButtonLayout.LayoutParams) targetView.getLayoutParams();
        layoutParams.width = Integer.parseInt(widthEdit.getText().toString());
        layoutParams.height = Integer.parseInt(heightEdit.getText().toString());
        layoutParams.offsetHorizontal = Integer.parseInt(horizOffsetEdit.getText().toString());
        layoutParams.offsetVertical = Integer.parseInt(vertOffsetEdit.getText().toString());
        layoutParams.alignmentHorizontal = getHorizontalAlignment();
        layoutParams.alignmentVertical = getVerticalAlignment();
        targetView.setLayoutParams(layoutParams);
    }

    protected LayoutEditable getEditTarget() {
        return targetView;
    }

    public void show(LayoutEditable view) {
        targetView = view;
        showDialog(view.getContext());
        loadLayoutSettings((LoadableButtonLayout.LayoutParams) view.getLayoutParams());
        loadSettings();
    }

    @Override
    protected void onExitWithSave() {
        applyLayoutSettings();
        saveSettings();
        targetView = null;
    }

    @Override
    protected void onExitDiscard() {

    }

    protected abstract void loadSettings();
    protected abstract void saveSettings();
}
