package git.artdeell.dnbootstrap.input.editor;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;

public interface LayoutEditable {
    Context getContext();
    ViewGroup.LayoutParams getLayoutParams();
    void setLayoutParams(ViewGroup.LayoutParams params);
    ViewParent getParent();
    View fullClone();

    LayoutEditorDialog createEditor();
    boolean isCompatibleEditor(LayoutEditorDialog editorDialog);
}
