package git.artdeell.dnbootstrap.input;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.GrabListener;
import git.artdeell.dnbootstrap.glfw.KeyCodes;
import git.artdeell.dnbootstrap.input.editor.JoystickEditorDialog;
import git.artdeell.dnbootstrap.input.editor.LayoutEditable;
import git.artdeell.dnbootstrap.input.editor.LayoutEditorDialog;
import git.artdeell.dnbootstrap.input.model.InputConfiguration;
import git.artdeell.dnbootstrap.input.model.JoystickData;
import git.artdeell.dnbootstrap.input.model.ViewCreator;
import git.artdeell.dnbootstrap.input.model.VisibilityConfiguration;

public class Joystick extends View implements LayoutTouchConsumer, LayoutEditable, Recreatable, GrabListener {
    public final JoystickData joystickData;
    private final Paint borderPaint;
    private final Paint fillPaint;
    private final Paint handlePaint;
    private float handleX, handleY;
    private float centerX, centerY;
    private float radius;
    private float handleRadius;
    private int lastSentKeyMask = 0;
    private static final int Up = 1;
    private static final int Left = 2;
    private static final int Down = 4;
    private static final int Right = 8;
    private static final int handleColor = 0x80ffffff;

    public Joystick(@NonNull Context context, JoystickData joystickData) {
        super(context, null, R.attr.joystickStyle);
        this.joystickData = joystickData;
        setLayoutParams(joystickData.layoutParams);

        borderPaint = new Paint();
        borderPaint.setColor(handleColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        fillPaint = new Paint();
        fillPaint.setColor(joystickData.backgroundColor);
        fillPaint.setStyle(Paint.Style.FILL);

        handlePaint = new Paint();
        handlePaint.setColor(handleColor);
        handlePaint.setStyle(Paint.Style.FILL);
    }

    public Joystick(@NonNull Context context) {
        this(context, (AttributeSet) null);
    }

    public Joystick(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.joystickStyle);
    }

    public Joystick(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        joystickData = new JoystickData();
        joystickData.inputConfiguration = new InputConfiguration();

        borderPaint = new Paint();
        borderPaint.setColor(handleColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        fillPaint = new Paint();
        fillPaint.setColor(joystickData.backgroundColor);
        fillPaint.setStyle(Paint.Style.FILL);

        handlePaint = new Paint();
        handlePaint.setColor(handleColor);
        handlePaint.setStyle(Paint.Style.FILL);

        joystickData.axisCodes = new int[2];
        joystickData.autoCenter = true;
        joystickData.layoutParams = new LoadableButtonLayout.LayoutParams(15, 15);
        joystickData.inputConfiguration.sticky = false;
    }

    public void applyStyling() {
        fillPaint.setColor(joystickData.backgroundColor);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f;
        handleRadius = radius * 0.25f;
        radius = radius - handleRadius;
        handleX = centerX;
        handleY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(centerX, centerY, radius, fillPaint);
        canvas.drawCircle(centerX, centerY, radius, borderPaint);

        canvas.drawCircle(handleX, handleY, handleRadius, handlePaint);
    }

    private void updateJoystickPosition(float x, float y) {
        float targetX = x + getLeft();
        float targetY = y + getTop();

        float dx = targetX - centerX;
        float dy = targetY - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > radius) {
            float ratio = radius / distance;
            targetX = centerX + dx * ratio;
            targetY = centerY + dy * ratio;
        }

        handleX = targetX;
        handleY = targetY;

        invalidate();

        float normalizedX = (handleX - centerX) / radius;
        float normalizedY = (handleY - centerY) / radius;

        updateKeyStates(normalizedX, normalizedY);
    }

    private void updateKeyStates(float normalizedX, float normalizedY) {
        int newKeyMask = 0;

        if (normalizedX < -0.3f) {
            newKeyMask |= Left;
        }
        if (normalizedX > 0.3f) {
            newKeyMask |= Right;
        }
        if (normalizedY < -0.3f) {
            newKeyMask |= Up;
        }
        if (normalizedY > 0.3f) {
            newKeyMask |= Down;
        }

        sendKeyChanges(lastSentKeyMask, newKeyMask);
        lastSentKeyMask = newKeyMask;
    }

    private void sendKeyChanges(int oldMask, int newMask) {
        int releasedKeys = oldMask & ~newMask;
        if ((releasedKeys & Up) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_W, KeyCodes.GLFW_RELEASE, 0);
        if ((releasedKeys & Left) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_A, KeyCodes.GLFW_RELEASE, 0);
        if ((releasedKeys & Down) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_S, KeyCodes.GLFW_RELEASE, 0);
        if ((releasedKeys & Right) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_D, KeyCodes.GLFW_RELEASE, 0);

        int pressedKeys = newMask & ~oldMask;
        if ((pressedKeys & Up) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_W, KeyCodes.GLFW_PRESS, 0);
        if ((pressedKeys & Left) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_A, KeyCodes.GLFW_PRESS, 0);
        if ((pressedKeys & Down) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_S, KeyCodes.GLFW_PRESS, 0);
        if ((pressedKeys & Right) != 0) GLFW.sendKeyEvent(KeyCodes.GLFW_KEY_D, KeyCodes.GLFW_PRESS, 0);
    }

    @Override
    public void onTouchState(boolean isTouched) {
        if (!isTouched && joystickData.autoCenter) {
            handleX = centerX;
            handleY = centerY;
            updateKeyStates(0, 0);
            invalidate();
        }
    }

    @Override
    public void onTouchPosition(float x, float y) {
        updateJoystickPosition(x, y);
    }

    @Override
    public View fullClone() {
        return new Joystick(getContext(), new JoystickData(this.joystickData));
    }

    @Override
    public LayoutEditorDialog createEditor() {
        return new JoystickEditorDialog();
    }

    @Override
    public boolean isCompatibleEditor(LayoutEditorDialog editorDialog) {
        return editorDialog instanceof JoystickEditorDialog;
    }

    @NonNull
    @Override
    public InputConfiguration getInputConfiguration() {
        return joystickData.inputConfiguration;
    }

    @Override
    public VisibilityConfiguration getVisibilityConfiguration() {
        return joystickData;
    }

    @NonNull
    @Override
    public ViewCreator getCreator() {
        return joystickData;
    }

    @Override
    public void onGrabState(boolean isGrabbing) {

    }
}
