package git.artdeell.dnbootstrap.input.model;

import android.content.Context;
import android.view.View;

/**
 * Objects that extend ViewCreator have enough data in them to create a view based on themselves
 */
public interface ViewCreator {
    View createView(Context context);
    String getType();
}
