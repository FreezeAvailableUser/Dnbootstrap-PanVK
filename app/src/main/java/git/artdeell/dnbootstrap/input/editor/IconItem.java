package git.artdeell.dnbootstrap.input.editor;

import android.content.Context;
import android.graphics.Color;
import android.widget.ImageView;

public class IconItem implements GridItem {
    private final String assetName;

    public IconItem(String assetName) {
        this.assetName = assetName;
    }

    @Override
    public void bind(ImageView view, Context context) {
        if ("None".equals(assetName) || assetName == null || assetName.isEmpty()) {
            view.setImageDrawable(null);
            view.setBackgroundColor(Color.LTGRAY);
        } else {
            int drawableId = context.getResources().getIdentifier(assetName, "drawable", context.getPackageName());
            if (drawableId != 0) {
                view.setImageResource(drawableId);
            } else {
                view.setImageDrawable(null);
            }
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public Object getValue() {
        return assetName;
    }
}
