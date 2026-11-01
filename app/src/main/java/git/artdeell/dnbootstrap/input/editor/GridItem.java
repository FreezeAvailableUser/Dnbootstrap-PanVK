package git.artdeell.dnbootstrap.input.editor;

import android.content.Context;
import android.widget.ImageView;

public interface GridItem {
    void bind(ImageView view, Context context);
    Object getValue();
}
