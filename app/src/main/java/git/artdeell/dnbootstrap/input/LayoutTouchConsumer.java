package git.artdeell.dnbootstrap.input;

import git.artdeell.dnbootstrap.input.model.InputConfiguration;
import git.artdeell.dnbootstrap.input.model.VisibilityConfiguration;

public interface LayoutTouchConsumer {
    void onTouchState(boolean isTouched);
    void onTouchPosition(float x, float y);
    void setVisibility(int visibility);
    int getLeft();
    int getTop();
    InputConfiguration getInputConfiguration();
    VisibilityConfiguration getVisibilityConfiguration();
}
