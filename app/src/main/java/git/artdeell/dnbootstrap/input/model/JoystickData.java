package git.artdeell.dnbootstrap.input.model;

import android.content.Context;
import android.view.View;

import java.util.Arrays;

import git.artdeell.dnbootstrap.input.Joystick;
import git.artdeell.dnbootstrap.input.LoadableButtonLayout;

public class JoystickData extends VisibilityConfiguration implements ViewCreator {
    public static final String TYPE = "joystick";
    public LoadableButtonLayout.LayoutParams layoutParams;
    public InputConfiguration inputConfiguration;
    public int[] axisCodes;
    public boolean autoCenter;
    public int backgroundColor;

    public JoystickData() {}

    public JoystickData(JoystickData src) {
        this.layoutParams = new LoadableButtonLayout.LayoutParams(src.layoutParams);
        this.inputConfiguration = new InputConfiguration(src.inputConfiguration);
        this.axisCodes = Arrays.copyOf(src.axisCodes, src.axisCodes.length);
        this.autoCenter = src.autoCenter;
        this.showInGame = src.showInGame;
        this.showInMenu = src.showInMenu;
        this.backgroundColor = src.backgroundColor;
    }

    public static JoystickData createDefault() {
        JoystickData joystickData = new JoystickData();
        joystickData.axisCodes = new int[2];
        joystickData.showInGame = true;
        joystickData.showInMenu = true;
        joystickData.layoutParams = new LoadableButtonLayout.LayoutParams(8, 8);
        joystickData.layoutParams.offsetHorizontal = 1;
        joystickData.layoutParams.offsetVertical = 6;
        joystickData.inputConfiguration = new InputConfiguration();
        joystickData.inputConfiguration.sticky = false;
        joystickData.autoCenter = true;
        joystickData.backgroundColor = 0;
        return joystickData;
    }

    @Override
    public View createView(Context context) {
        return new Joystick(context, this);
    }

    @Override
    public String getType() {
        return JoystickData.TYPE;
    }
}
