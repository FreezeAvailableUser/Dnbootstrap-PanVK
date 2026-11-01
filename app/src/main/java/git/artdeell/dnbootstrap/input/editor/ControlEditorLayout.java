package git.artdeell.dnbootstrap.input.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.glfw.KeyCodes;
import git.artdeell.dnbootstrap.input.ControlButton;
import git.artdeell.dnbootstrap.input.Recreatable;
import git.artdeell.dnbootstrap.input.LoadableButtonLayout;
import git.artdeell.dnbootstrap.input.model.ControlButtonData;
import git.artdeell.dnbootstrap.input.model.LayoutDescription;
import git.artdeell.dnbootstrap.input.model.ViewCreator;

public class ControlEditorLayout extends LoadableButtonLayout {
    private final GridDrawable background;
    private final GlobalSettingsDialog globalSettings;
    private int gridWidth, gridHeight;
    protected boolean autoAlign = true;
    protected boolean cursorToTouch = false;
    protected float gridPitchDp;
    protected LayoutEditorHost layoutEditorHost;
    private LayoutEditorDialog editorDialog;

    public ControlEditorLayout(@NonNull Context context) {
        this(context, null);
    }

    public ControlEditorLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlEditorLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ControlEditorLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setBackground(background = new GridDrawable());
        globalSettings = new GlobalSettingsDialog(this);
        setOnClickListener((v)->globalSettings.show(getContext()));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(editorDialog != null) editorDialog.hide();
        globalSettings.hide();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        child.setOnTouchListener(new EditModeTouchListener());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        background.updateBounds();
        gridWidth = (r - l) / gridPitch;
        gridHeight = (b - t) / gridPitch;
    }

    private static int clampPos(int val, int max) {
        if(val < 0) return 0;
        return Math.min(val, max);
    }

    private void showEditorDialog(View target) {
        if(editorDialog != null) editorDialog.hide();
        if(!(target instanceof LayoutEditable)) return;
        LayoutEditable editable = (LayoutEditable) target;
        if(editorDialog == null || !editable.isCompatibleEditor(editorDialog)) {
            editorDialog = editable.createEditor();
        }
        editorDialog.show(editable);
    }

    @Override
    public void setGridPitch(float gridPitchDp) {
        super.setGridPitch(gridPitchDp);
        this.gridPitchDp = gridPitchDp;
    }

    private boolean checkLayoutOpenButton(View child) {
        if(!(child instanceof ControlButton)) return false;
        ControlButtonData data = ((ControlButton)child).controlButtonData;
        if(!data.showInMenu || !data.showInGame) return false;
        for(int i = 0; i < data.keyCodes.length; i++) {
            if(data.keyCodes[i] == KeyCodes.SPECIAL_KEY_OPEN_EDITOR) return true;
        }
        return false;
    }

    public LayoutDescription save() {
        int childCount = getChildCount();
        ArrayList<ViewCreator> children = new ArrayList<>(childCount);
        boolean hasEditorOpenButton = false;
        for(int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if(!(child instanceof Recreatable)) continue;
            if(!hasEditorOpenButton) hasEditorOpenButton = checkLayoutOpenButton(child);
            children.add(((Recreatable)child).getCreator());
        }
        children.trimToSize();
        if(!hasEditorOpenButton) {
            Toast.makeText(getContext(), R.string.grid_layout_global_must_have_editor_btn, Toast.LENGTH_LONG).show();
            return null;
        }
        LayoutDescription layoutDescription = new LayoutDescription();
        layoutDescription.buttonList = children;
        layoutDescription.gridPitch = gridPitchDp;
        layoutDescription.cursorToTouch = cursorToTouch;
        return layoutDescription;
    }

    public boolean saveAsync() {
        LayoutDescription layoutDescription = save();
        if(layoutDescription == null) return false;
        File layoutPath = new File(getContext().getFilesDir(), "layout.json");
        new Thread(()->{
            Gson gson = controlsGson().setPrettyPrinting().create();
            try(FileOutputStream fileOutputStream = new FileOutputStream(layoutPath)) {
                fileOutputStream.write(gson.toJson(layoutDescription).getBytes(StandardCharsets.UTF_8));
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        return true;
    }

    public void setEditorHost(LayoutEditorHost layoutEditorHost) {
        this.layoutEditorHost = layoutEditorHost;
    }

    private class EditModeTouchListener implements View.OnTouchListener {
        private float downRawX, downRawY, downX, downY;
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            switch (motionEvent.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    downRawX = motionEvent.getRawX();
                    downRawY = motionEvent.getRawY();
                    downX = downRawX - view.getX();
                    downY = downRawY - view.getY();
                } break;
                case MotionEvent.ACTION_MOVE: {
                    float newX = motionEvent.getRawX() - downX;
                    float newY = motionEvent.getRawY() - downY;
                    int newGridX = clampPos((int)(newX / gridPitch), gridWidth - layoutParams.width);
                    int newGridY = clampPos((int)(newY / gridPitch), gridHeight - layoutParams.height);
                    view.setX(newGridX * gridPitch);
                    view.setY(newGridY * gridPitch);
                } break;
                case MotionEvent.ACTION_UP: {
                    float lastRawX = motionEvent.getRawX();
                    float lastRawY = motionEvent.getRawY();
                    if(Math.abs(lastRawX - downRawX) < gridPitch && Math.abs(lastRawY - downRawY) < gridPitch) {
                        showEditorDialog(view);
                    }else {
                        applyViewPosition(view, layoutParams);
                    }
                    view.setTranslationX(0);
                    view.setTranslationY(0);
                }
            }
            return true;
        }

        private void applyViewPosition(View view, LayoutParams layoutParams) {
            int gridX = (int) (view.getX() / gridPitch);
            int gridY = (int) (view.getY() / gridPitch);
            boolean autoAlign = ControlEditorLayout.this.autoAlign;
            if(layoutParams.alignmentVertical == LayoutParams.ALIGNMENT_CENTER || layoutParams.alignmentHorizontal == LayoutParams.ALIGNMENT_CENTER) {
                autoAlign = true;
            }
            if(autoAlign) {
                int halfwayX = getWidth() / 2;
                int halfwayY = getHeight() / 2;
                layoutParams.alignmentHorizontal = view.getX() < halfwayX ? LayoutParams.ALIGNMENT_LEFT : LayoutParams.ALIGNMENT_RIGHT;
                layoutParams.alignmentVertical = view.getY () < halfwayY ? LayoutParams.ALIGNMENT_TOP : LayoutParams.ALIGNMENT_BOTTOM;
            }
            switch (layoutParams.alignmentHorizontal) {
                case LayoutParams.ALIGNMENT_LEFT:
                    layoutParams.offsetHorizontal = gridX;
                    break;
                case LayoutParams.ALIGNMENT_RIGHT:
                    layoutParams.offsetHorizontal = gridWidth - gridX - layoutParams.width;
                    break;
            }
            switch (layoutParams.alignmentVertical) {
                case LayoutParams.ALIGNMENT_TOP:
                    layoutParams.offsetVertical = gridY;
                    break;
                case LayoutParams.ALIGNMENT_BOTTOM:
                    layoutParams.offsetVertical = gridHeight - gridY - layoutParams.height;
                    break;
            }
            view.setLayoutParams(layoutParams);
        }
    }

    private class GridDrawable extends Drawable {
        private static final int defaultLineColor = 0xFF1cc7ed;
        private static final int edgeLineColor = Color.RED;
        private final Paint linePaint = new Paint();
        private final Paint edgePaint = new Paint();
        private final Rect edgeRect = new Rect();
        private final Rect bounds = new Rect();
        public GridDrawable() {
            linePaint.setColor(defaultLineColor);
            linePaint.setStrokeWidth(1f);
            edgePaint.setColor(edgeLineColor);
            edgePaint.setStyle(Paint.Style.STROKE);
            edgePaint.setStrokeWidth(1f);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int gridHLines = bounds.width() / gridPitch;
            int gridVLines = bounds.height() / gridPitch;
            for(int h = 1; h < gridHLines; h++) {
                int displayLine = bounds.left + h * gridPitch;
                canvas.drawLine(displayLine, edgeRect.top, displayLine, edgeRect.bottom, linePaint);
            }

            for(int v = 1; v <= gridVLines; v++) {
                int displayLine = bounds.top + v * gridPitch;
                canvas.drawLine(edgeRect.left, displayLine, edgeRect.right, displayLine, linePaint);
            }

            canvas.drawRect(edgeRect, edgePaint);
        }

        public void updateBounds() {
            bounds.set(getBounds());
            bounds.offset(paddingLeft, paddingTop);
            edgeRect.set(bounds);
            edgeRect.right = edgeRect.left + ((edgeRect.width() / gridPitch) * gridPitch);
            edgeRect.bottom = edgeRect.top + ((edgeRect.height() / gridPitch) * gridPitch);
        }

        @Override
        public void setBounds(@NonNull Rect bounds) {
            super.setBounds(bounds);
            updateBounds();
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            updateBounds();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int i) {

        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {

        }
    }
}
