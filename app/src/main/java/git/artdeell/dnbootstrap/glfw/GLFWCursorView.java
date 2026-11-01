package git.artdeell.dnbootstrap.glfw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import git.artdeell.dnbootstrap.R;

public class GLFWCursorView extends View implements CursorImplementor {
    private Drawable cursorDrawable;
    private final Rect cursorRect = new Rect();
    private final Paint customCursorPaint = new Paint();
    private boolean noDraw = false;

    public GLFWCursorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public GLFWCursorView(Context context) {
        super(context);
        init(context);
    }

    public GLFWCursorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GLFWCursorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        GLFW.setCursorImpl(this);
        cursorDrawable = context.getDrawable(R.drawable.ic_mouse_pointer);
        if(cursorDrawable != null) {
            cursorDrawable.setBounds(0, 0, 36, 54);
            cursorRect.set(cursorDrawable.getBounds());
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if(noDraw) return;
        canvas.translate((int)(GLFW.cursorX * getWidth()), (int)(GLFW.cursorY * getHeight()));
        GLFWCursor cursor = GLFW.getCursor();
        if(cursor == null) {
            cursorDrawable.draw(canvas);
        }else {
            canvas.scale(1.15f, 1.15f);
            canvas.drawBitmap(cursor.bitmap, -cursor.hotX, -cursor.hotY, customCursorPaint);
        }
    }

    @Override
    public void onCursorPosition() {
        if(!noDraw) post(this::invalidate);
    }

    @Override
    public void onCursorChanged() {
        post(this::invalidate);
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        noDraw = isGrabbing;
        post(this::invalidate);
    }
}
