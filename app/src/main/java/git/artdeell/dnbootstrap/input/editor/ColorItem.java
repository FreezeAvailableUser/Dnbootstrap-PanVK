package git.artdeell.dnbootstrap.input.editor;

import android.content.Context;
import android.widget.ImageView;

public class ColorItem implements GridItem {
    private final int color;

    public ColorItem(int color) {
        this.color = color;
    }

    @Override
    public void bind(ImageView view, Context context) {
        view.setImageDrawable(null); // Clear any icon
        view.setBackgroundColor(color);
    }

    @Override
    public Object getValue() {
        return color;
    }
}
