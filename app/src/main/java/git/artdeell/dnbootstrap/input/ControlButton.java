package git.artdeell.dnbootstrap.input;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GrabListener;
import git.artdeell.dnbootstrap.glfw.KeyCodes;
import git.artdeell.dnbootstrap.glfw.MouseCodes;
import git.artdeell.dnbootstrap.input.editor.ButtonEditorDialog;
import git.artdeell.dnbootstrap.input.editor.LayoutEditable;
import git.artdeell.dnbootstrap.input.editor.LayoutEditorDialog;
import git.artdeell.dnbootstrap.input.editor.LayoutEditorHost;
import git.artdeell.dnbootstrap.input.model.ControlButtonData;
import git.artdeell.dnbootstrap.input.model.InputConfiguration;
import git.artdeell.dnbootstrap.input.model.ViewCreator;
import git.artdeell.dnbootstrap.input.model.VisibilityConfiguration;

public class ControlButton extends androidx.appcompat.widget.AppCompatTextView implements LayoutTouchConsumer, LayoutEditable, Recreatable, GrabListener {
    public final ControlButtonData controlButtonData;
    private boolean firstTouch = true;
    private double initialCursorX;
    private double initialCursorY;
    private float initialTouchX, initialTouchY;

    public ControlButton(@NonNull Context context, ControlButtonData controlButtonData) {
        super(context, null, R.attr.controlButtonStyle);
        this.controlButtonData = controlButtonData;
        setLayoutParams(controlButtonData.layoutParams);
        setText(controlButtonData.label);
        applyBackground();
    }

    public ControlButton(@NonNull Context context) {
        this(context, (AttributeSet) null);
    }

    public ControlButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.controlButtonStyle);
    }

    public ControlButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        controlButtonData = new ControlButtonData();
        controlButtonData.inputConfiguration = new InputConfiguration();
        if(defStyleAttr == 0) defStyleAttr = R.attr.controlButtonStyle;
        try(TypedArray attrArray = context.obtainStyledAttributes(attrs, R.styleable.ControlButton, defStyleAttr, R.style.ControlButton)) {
            controlButtonData.keyCodes = new int[2];
            controlButtonData.keyCodes[0] = attrArray.getInteger(R.styleable.ControlButton_button_keycode, 0);
            controlButtonData.keyCodes[1] = attrArray.getInteger(R.styleable.ControlButton_button_keycode2, 0);
            controlButtonData.inputConfiguration.sticky  = attrArray.getBoolean(R.styleable.ControlButton_sticky, true);
            controlButtonData.inputConfiguration.movesCursor = attrArray.getBoolean(R.styleable.ControlButton_cursorPassThru, false);
            controlButtonData.label = getText().toString();
        }
    }

    private void callSpecialCallback(Context context, int code, int state) {
        if(state == KeyCodes.GLFW_PRESS) return;
        switch (code) {
            case KeyCodes.SPECIAL_KEY_OPEN_KEYBOARD:
                if(context instanceof SoftInputCallback) {
                    ((SoftInputCallback)context).requestSoftInput();
                }
                break;
            case KeyCodes.SPECIAL_KEY_OPEN_EDITOR:
                if(context instanceof LayoutEditorHost) {
                    ((LayoutEditorHost)context).openLayoutEditor();
                }
                break;
        }
    }

    public void executeKeyEvent(int code, int state) {
        if(code == 0) return;
        if(code < 0) {
            switch (code) {
                case KeyCodes.SPECIAL_KEY_MOUSE_LEFT:
                    GLFW.sendMouseEvent(MouseCodes.GLFW_MOUSE_BUTTON_LEFT, state, 0);
                    break;
                case KeyCodes.SPECIAL_KEY_MOUSE_RIGHT:
                    GLFW.sendMouseEvent(MouseCodes.GLFW_MOUSE_BUTTON_RIGHT, state, 0);
                    break;
                case KeyCodes.SPECIAL_KEY_MOUSE_MIDDLE:
                    GLFW.sendMouseEvent(MouseCodes.GLFW_MOUSE_BUTTON_MIDDLE, state, 0);
                    break;
                default:
                    callSpecialCallback(getContext(), code, state);
            }
        }else if(code <= KeyCodes.GLFW_KEY_LAST) {
            GLFW.sendKeyEvent(code, state, 0);
        }
    }

    @Override
    public void onTouchState(boolean isTouched) {
        int state = isTouched ? KeyCodes.GLFW_PRESS : KeyCodes.GLFW_RELEASE;
        for(int keyCode : controlButtonData.keyCodes) {
            executeKeyEvent(keyCode, state);
        }
        if (isTouched) {
            firstTouch = true;
        }
    }

    @Override
    public void onTouchPosition(float x, float y) {
        if (controlButtonData.inputConfiguration.movesCursor) {
            View rootView = getRootView();
            if (rootView != null) {
                if (firstTouch) {
                    initialCursorX = GLFW.cursorX;
                    initialCursorY = GLFW.cursorY;
                    initialTouchX = x;
                    initialTouchY = y;
                    firstTouch = false;
                }
                GLFW.cursorX = initialCursorX + (x - initialTouchX) / rootView.getWidth();
                GLFW.cursorY = initialCursorY + (y - initialTouchY) / rootView.getHeight();
                GLFW.sendMousePos();
            }
        }
    }

    @Override
    public View fullClone() {
        return new ControlButton(getContext(), new ControlButtonData(this.controlButtonData));
    }

    @Override
    public LayoutEditorDialog createEditor() {
        return new ButtonEditorDialog();
    }

    @Override
    public boolean isCompatibleEditor(LayoutEditorDialog editorDialog) {
        return editorDialog instanceof ButtonEditorDialog;
    }

    @NonNull
    @Override
    public InputConfiguration getInputConfiguration() {
        return controlButtonData.inputConfiguration;
    }

    @Override
    public VisibilityConfiguration getVisibilityConfiguration() {
        return controlButtonData;
    }

    @NonNull
    @Override
    public ViewCreator getCreator() {
        return controlButtonData;
    }

    @Override
    public void onGrabState(boolean isGrabbing) {

    }

    public void applyBackground() {
        setBackground(null);

        if (controlButtonData.backgroundAssetId != null &&
                !controlButtonData.backgroundAssetId.isEmpty() &&
                !controlButtonData.backgroundAssetId.equals("None")) {
            int drawableId = getResources().getIdentifier(
                    controlButtonData.backgroundAssetId,
                    "drawable",
                    getContext().getPackageName()
            );
            if (drawableId != 0) {
                Drawable assetDrawable = AppCompatResources.getDrawable(getContext(), drawableId);

                if(controlButtonData.backgroundColor != 0) {
                    android.graphics.drawable.ColorDrawable colorDrawable =
                            new android.graphics.drawable.ColorDrawable(controlButtonData.backgroundColor);
                    android.graphics.drawable.LayerDrawable layerDrawable =
                            new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{
                                    colorDrawable,
                                    assetDrawable
                            });
                    setBackground(layerDrawable);
                    return;
                }
                setBackgroundDrawable(assetDrawable);
            }
        } else if (controlButtonData.backgroundColor != 0) {
            setBackgroundColor(controlButtonData.backgroundColor);
        }
    }
}
