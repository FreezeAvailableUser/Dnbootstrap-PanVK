package git.artdeell.dnbootstrap.input.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

import git.artdeell.dnbootstrap.R;

public abstract class PropertyDialog {
    private final int viewId;
    private final int titleId;
    private AlertDialog dialog;


    public PropertyDialog(int viewId, int titleId) {
        this.viewId = viewId;
        this.titleId = titleId;
    }

    protected void createAndShow(Context context) {
        dialog = new AlertDialog.Builder(context)
                .setView(viewId)
                .setTitle(titleId)
                .setPositiveButton(android.R.string.ok, (d, w)->{
                    onExitWithSave();
                })
                .setOnCancelListener((d)->onExitDiscard())
                .show();
        inflate(dialog);
        dialog.show();
    }

    protected void showDialog(Context context) {
        if(dialog == null) createAndShow(context);
        else dialog.show();
    }

    public void hide() {
        if(dialog != null) dialog.cancel();
    }

    protected abstract void inflate(Dialog dialog);
    protected abstract void onExitWithSave();
    protected abstract void onExitDiscard();
}
