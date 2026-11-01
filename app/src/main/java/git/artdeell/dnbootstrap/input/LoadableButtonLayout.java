package git.artdeell.dnbootstrap.input;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import git.artdeell.dnbootstrap.R;
import git.artdeell.dnbootstrap.input.model.LayoutDescription;
import git.artdeell.dnbootstrap.input.model.ViewCreator;
import git.artdeell.dnbootstrap.input.model.ViewCreatorHelper;


public class LoadableButtonLayout extends ViewGroup {
    protected int gridPitch;
    protected int paddingLeft = 0;
    protected int paddingTop = 0;

    public LoadableButtonLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setGridPitch(10);
        loadAsync();
    }

    public LoadableButtonLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LoadableButtonLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadableButtonLayout(@NonNull Context context) {
        this(context, null);
    }

    protected static GsonBuilder controlsGson() {
        return new GsonBuilder()
                .registerTypeAdapter(ViewCreator.class, new ViewCreatorHelper());
    }

    private void loadFromStream(InputStream inputStream) throws Exception {
        Gson gson = controlsGson().create();
        LayoutDescription layoutDescription;
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)){
            layoutDescription = gson.fromJson(inputStreamReader, LayoutDescription.class);
        }
        post(()->load(layoutDescription));

    }

    private void tryLoadLayout() throws Exception {
        Context context = getContext();
        File layoutPath = new File(context.getFilesDir(), "layout.json");
        try (FileInputStream fileInputStream = new FileInputStream(layoutPath)) {
            loadFromStream(fileInputStream);
            return;
        } catch (IOException ignored) {}
        AssetManager assetManager = context.getAssets();
        try(InputStream defaultLayoutStream = assetManager.open("layout-default.json")) {
            loadFromStream(defaultLayoutStream);
        }
    }
    public void loadAsync() {
        new Thread(()-> {
            try {
                tryLoadLayout();
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void load(LayoutDescription description) {
        Context context = getContext();
        removeAllViews();
        setGridPitch(description.gridPitch);
        for(ViewCreator creator : description.buttonList) {
            addView(creator.createView(context));
        }
    }

    @Override
    public void removeAllViews() {
        onRemoveAllViews();
        super.removeAllViews();
    }

    private boolean isExactSize(int dim) {
        return dim != LayoutParams.WRAP_CONTENT && dim != LayoutParams.MATCH_PARENT;
    }

    private void layoutChild(View child, int w, int h) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        if(!isExactSize(layoutParams.width) || !isExactSize(layoutParams.height))
            throw new RuntimeException("Exact view size is required for layout");
        int childWidth = layoutParams.width * gridPitch;
        int childHeight = layoutParams.height * gridPitch;
        int childMeasuredWidth = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childMeasuredHeight = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        child.measure(childMeasuredWidth, childMeasuredHeight);
        if(child.getMeasuredWidth() != childWidth && child.getMeasuredHeight() != childHeight)
            throw new RuntimeException("Child measure must return same dimensions");

        int childLeft = computeChildLeft(w, layoutParams);

        int childTop = computeChildTop(h, layoutParams);

        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }

    private int computeChildLeft(int w, LayoutParams layoutParams) {
        int childLeft;
        switch (layoutParams.alignmentHorizontal) {
            case LayoutParams.ALIGNMENT_LEFT:
                childLeft = layoutParams.offsetHorizontal * gridPitch;
                break;
            case LayoutParams.ALIGNMENT_RIGHT:
                childLeft = (w - layoutParams.offsetHorizontal - layoutParams.width) * gridPitch;
                break;
            case LayoutParams.ALIGNMENT_CENTER:
                childLeft = (w / 2 + layoutParams.offsetHorizontal) * gridPitch;
                break;
            default:
                throw new RuntimeException("Unknown horizontal alignment type "+ layoutParams.alignmentVertical);
        }

        childLeft += paddingLeft;
        return childLeft;
    }

    private int computeChildTop(int h, LayoutParams layoutParams) {
        int childTop;
        switch (layoutParams.alignmentVertical) {
            case LayoutParams.ALIGNMENT_TOP:
                childTop = layoutParams.offsetVertical * gridPitch;
                break;
            case LayoutParams.ALIGNMENT_BOTTOM:
                childTop = (h - layoutParams.offsetVertical - layoutParams.height) * gridPitch;
                break;
            case LayoutParams.ALIGNMENT_CENTER:
                childTop = (h / 2 + layoutParams.offsetVertical) * gridPitch;
                break;
            default:
                throw new RuntimeException("Unknown vertical alignment type "+ layoutParams.alignmentVertical);
        }

        childTop += paddingTop;
        return childTop;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(6, 6);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = (r - l);
        int h = (b - t);
        paddingLeft = (w % gridPitch) / 2;
        paddingTop = (h % gridPitch) / 2;
        int wScaled = w / gridPitch;
        int hScaled = h / gridPitch;
        invalidate();
        for(int i = 0; i < getChildCount(); i++) {
            layoutChild(getChildAt(i), wScaled, hScaled);
        }
    }

    protected void onRemoveAllViews() {

    }

    public void setGridPitch(float gridPitchDp) {
        gridPitch = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gridPitchDp, getContext().getResources().getDisplayMetrics());
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public static final int ALIGNMENT_LEFT = 0;
        public static final int ALIGNMENT_TOP = 1;
        public static final int ALIGNMENT_RIGHT = 2;
        public static final int ALIGNMENT_BOTTOM = 3;
        public static final int ALIGNMENT_CENTER = 4;
        public int alignmentHorizontal = ALIGNMENT_LEFT;
        public int alignmentVertical = ALIGNMENT_TOP;
        public int offsetHorizontal;
        public int offsetVertical;
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(LoadableButtonLayout.LayoutParams source) {
            super(source);
            alignmentHorizontal = source.alignmentHorizontal;
            alignmentVertical = source.alignmentVertical;
            offsetHorizontal = source.offsetHorizontal;
            offsetVertical = source.offsetVertical;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            try(TypedArray typedArray = c.obtainStyledAttributes(attrs, R.styleable.ControlGridLayout_LayoutParams)) {
                alignmentHorizontal = typedArray.getInt(R.styleable.ControlGridLayout_LayoutParams_alignmentHorizontal, ALIGNMENT_LEFT);
                alignmentVertical = typedArray.getInt(R.styleable.ControlGridLayout_LayoutParams_alignmentVertical, ALIGNMENT_TOP);
                offsetHorizontal = typedArray.getInt(R.styleable.ControlGridLayout_LayoutParams_offsetHorizontal, 0);
                offsetVertical = typedArray.getInt(R.styleable.ControlGridLayout_LayoutParams_offsetVertical, 0);
            }
        }
    }
}
