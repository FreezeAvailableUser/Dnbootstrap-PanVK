package git.artdeell.dnbootstrap.input;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import git.artdeell.dnbootstrap.glfw.GLFW;
import git.artdeell.dnbootstrap.glfw.KeyCodes;
import git.artdeell.dnbootstrap.glfw.GrabListener;
import git.artdeell.dnbootstrap.glfw.MouseCodes;
import git.artdeell.dnbootstrap.input.model.InputConfiguration;
import git.artdeell.dnbootstrap.input.model.LayoutDescription;
import git.artdeell.dnbootstrap.input.model.ViewCreator;
import git.artdeell.dnbootstrap.input.model.VisibilityConfiguration;

public class ControlLayout extends LoadableButtonLayout implements GrabListener {
    private final Rect hitTestRect = new Rect();
    private final HashMap<Integer, HitTarget> lastHitTargets = new HashMap<>();
    private final Set<HitTarget> allHitTargets = new HashSet<>();
    private final HitTarget defaultHitTarget = new HitTarget(new DefaultConsumer());
    private Context cont = getContext();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public boolean cursorToTouch = false;

    public ControlLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        GLFW.addGrabListener(this);
    }

    public ControlLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ControlLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlLayout(@NonNull Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    private HitTarget hitTest(int x, int y) {
        for(HitTarget hitTarget : allHitTargets) {
            View child = ((View)hitTarget.consumer);
            if (child.getVisibility() != View.VISIBLE) continue; // ignore hidden views
            hitTestRect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            if(hitTestRect.contains(x,y)) return hitTarget;
        }
        return defaultHitTarget;
    }

    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        if(!(view instanceof LayoutTouchConsumer)) return;
        LayoutTouchConsumer layoutTouchConsumer = (LayoutTouchConsumer)view;
        allHitTargets.add(new HitTarget(layoutTouchConsumer));
        updateVisibility(layoutTouchConsumer);
    }

    @Override
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        if(allHitTargets.isEmpty()) return;
        if(!(view instanceof LayoutTouchConsumer)) return;
        Iterator<HitTarget> iter = allHitTargets.iterator();
        while(iter.hasNext()) {
            if(iter.next().consumer != view) continue;
            iter.remove();
            break;
        }
    }

    @Override
    protected void onRemoveAllViews() {
        super.onRemoveAllViews();
        allHitTargets.clear();
    }

    public void load(LayoutDescription description) {
        Context context = getContext();
        removeAllViews();
        setGridPitch(description.gridPitch);
        for(ViewCreator creator : description.buttonList) {
            addView(creator.createView(context));
        }
        this.cursorToTouch = description.cursorToTouch;
    }

    private void processPointer(MotionEvent event, int pointer, int action) {
        int pointerId = event.getPointerId(pointer);
        HitTarget lastHit = lastHitTargets.get(pointerId);
        float x = event.getX(pointer), y = event.getY(pointer);

        if(action == MotionEvent.ACTION_MOVE) {
            // Always update position for the INITIAL target only (no re-hit-testing)
            if (lastHit != null && lastHit.isInitialTarget) {
                lastHit.onTouchPosition(pointerId, x - lastHit.consumer.getLeft(),
                        y - lastHit.consumer.getTop());
            }
        }else if(action == MotionEvent.ACTION_POINTER_UP) {
            if(lastHit != null) lastHit.onTouchState(pointerId, false);
            lastHitTargets.remove(pointerId);
        }else if(action == MotionEvent.ACTION_POINTER_DOWN) {
            HitTarget hit = hitTest((int) x, (int) y);
            if(hit != null) {
                hit.isInitialTarget = true;  // Mark as owning this pointer
                hit.onTouchState(pointerId, true);
                hit.onTouchPosition(pointerId, x - hit.consumer.getLeft(), y - hit.consumer.getTop());
            }
            lastHitTargets.put(pointerId, hit);
        }
    }

    private void releaseAllPointers() {
        for(HitTarget target : lastHitTargets.values()) {
            if(target == null) continue;
            target.reset();
        }
        lastHitTargets.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int affectedPointer = -1;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                releaseAllPointers();
                processPointer(event, 0, MotionEvent.ACTION_POINTER_DOWN);
                break;
            case MotionEvent.ACTION_UP:
                releaseAllPointers();
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_POINTER_DOWN:
                affectedPointer = event.getActionIndex();
            case MotionEvent.ACTION_MOVE:
                for(int i = 0; i < event.getPointerCount(); i++) {
                    int reportedAction = MotionEvent.ACTION_MOVE;
                    if(affectedPointer == i) reportedAction = action;
                    processPointer(event, i, reportedAction);
                }
                break;
        }
        return true;
    }

    @Override
    public void onGrabState(boolean isGrabbing) {
        post(this::updateVisibility);
    }

    private void updateVisibility(LayoutTouchConsumer layoutTouchConsumer) {
        boolean isGrabbing = GLFW.isGrabbing();
        VisibilityConfiguration visibilityConfiguration = layoutTouchConsumer.getVisibilityConfiguration();
        boolean visible = visibilityConfiguration.showInGame && isGrabbing;
        visible |= visibilityConfiguration.showInMenu && !isGrabbing;
        layoutTouchConsumer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateVisibility() {
        for(HitTarget hitTarget : allHitTargets) {
            if(hitTarget == defaultHitTarget) continue;
            updateVisibility(hitTarget.consumer);
        }
    }

    private class HitTarget {
        public final @NonNull LayoutTouchConsumer consumer;
        private int firstTouchedPointer;
        private boolean lastState;
        private boolean isInitialTarget = false;

        private HitTarget(@NonNull LayoutTouchConsumer consumer) {
            this.consumer = consumer;
            this.firstTouchedPointer = -1;
            this.isInitialTarget = false;
        }

        public void onTouchState(int pointerId, boolean isTouched) {
            if(pointerId != firstTouchedPointer && firstTouchedPointer != -1) return;
            if(!isTouched) firstTouchedPointer = -1;
            if(isTouched && firstTouchedPointer == -1) firstTouchedPointer = pointerId;
            if(isTouched != lastState) {
                lastState = isTouched;
                consumer.onTouchState(isTouched);
            }
        }

        public void onTouchPosition(int pointerId, float x, float y) {
            if(pointerId != firstTouchedPointer) return;
            consumer.onTouchPosition(x - consumer.getLeft(), y - consumer.getTop());
        }

        public void reset() {
            firstTouchedPointer = -1;
            isInitialTarget = false;
            if(lastState) consumer.onTouchState(false);
            lastState = false;
        }
    }

    public class DefaultConsumer implements LayoutTouchConsumer {
        private final InputConfiguration defaultConfiguration = new InputConfiguration();
        private boolean deltaReady = false;
        private float lastX, lastY;
        private long touchDownTime = 0;
        private boolean isLongPress = false;
        private static final long LONG_PRESS_THRESHOLD = 200; // milliseconds
        private float touchDownX = 0f, touchDownY = 0f;
        private boolean touchMovedTooFar = false;
        private static final float MOVE_SLOP = 20f; // pixels


        @Override
        public void onTouchState(boolean isTouched) {
            if(isTouched) {
                // Touch down: record the time and reset movement state
                touchDownTime = System.currentTimeMillis();
                isLongPress = false;
                touchMovedTooFar = false;
                deltaReady = false;
            } else {
                // Touch up: handle breaking/placing logic
                long touchDuration = System.currentTimeMillis() - touchDownTime;

                if(isLongPress) {
                    // Release left mouse button (breaking)
                    GLFW.sendMouseEvent(MouseCodes.GLFW_MOUSE_BUTTON_LEFT, KeyCodes.GLFW_RELEASE, 0);
                } else if(touchDuration < LONG_PRESS_THRESHOLD && !touchMovedTooFar) {
                    // Short tap: send right mouse button (placing) in game or left mouse button in menu/inventory
                    int shortTouch;
                    if(!GLFW.isGrabbing()) {
                        shortTouch = MouseCodes.GLFW_MOUSE_BUTTON_LEFT;
                    } else {
                        shortTouch = MouseCodes.GLFW_MOUSE_BUTTON_RIGHT;
                    }
                    GLFW.sendMouseEvent(shortTouch, KeyCodes.GLFW_PRESS, 0);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            GLFW.sendMouseEvent(shortTouch, KeyCodes.GLFW_RELEASE, 0);
                        }
                    }, 20);
                }
            }
            lastX = lastY = 0;
            deltaReady = false;
        }

        @Override
        public void onTouchPosition(float x, float y) {
            if(!deltaReady) {
                lastX = x;
                lastY = y;
                touchDownX = x;
                touchDownY = y;
                deltaReady = true;

                // If using Direct Touch, snap cursor to finger immediately on touch down
                if(cursorToTouch && !GLFW.isGrabbing()) {
                    GLFW.cursorX = x / getWidth();
                    GLFW.cursorY = y / getHeight();
                    GLFW.sendMousePos();
                }
                return;
            }

            // If the finger has moved too far from initial down position, mark it
            float dxFromDown = x - touchDownX;
            float dyFromDown = y - touchDownY;
            if(!touchMovedTooFar && (dxFromDown * dxFromDown + dyFromDown * dyFromDown) > MOVE_SLOP * MOVE_SLOP) {
                touchMovedTooFar = true;
            }

            // Check if touch duration exceeded threshold for long press detection and hasn't moved too far
            if(!isLongPress && !touchMovedTooFar && System.currentTimeMillis() - touchDownTime >= LONG_PRESS_THRESHOLD) {
                isLongPress = true;
                // Send left mouse button press (breaking)
                GLFW.sendMouseEvent(MouseCodes.GLFW_MOUSE_BUTTON_LEFT, KeyCodes.GLFW_PRESS, 0);
            }

            if(GLFW.isGrabbing()) {
                // FPS MODE (Looking Around):
                // Always use Relative Drag to control camera smoothly.
                float deltaX = x - lastX;
                float deltaY = y - lastY;
                GLFW.cursorX += deltaX / getWidth();
                GLFW.cursorY += deltaY / getHeight();
                GLFW.sendMousePos();
            } else if(cursorToTouch) {
                // MENU MODE (Direct Touch):
                // Cursor follows finger absolutely.
                GLFW.cursorX = x / getWidth();
                GLFW.cursorY = y / getHeight();
                GLFW.sendMousePos();
            } else {
                // MENU MODE (Drag Mode):
                // Cursor moves relative to finger.
                float deltaX = x - lastX;
                float deltaY = y - lastY;
                GLFW.cursorX += deltaX / getWidth();
                GLFW.cursorY += deltaY / getHeight();
                GLFW.sendMousePos();
            }

            lastX = x;
            lastY = y;
        }

        @Override
        public void setVisibility(int visibility) {}

        @Override
        public int getLeft() {
            return 0;
        }

        @Override
        public int getTop() {
            return 0;
        }

        @NonNull
        @Override
        public InputConfiguration getInputConfiguration() {
            return defaultConfiguration;
        }

        @Override
        public VisibilityConfiguration getVisibilityConfiguration() {
            return null;
        }
    }
}