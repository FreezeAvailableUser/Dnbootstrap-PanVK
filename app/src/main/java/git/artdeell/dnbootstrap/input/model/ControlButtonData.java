package git.artdeell.dnbootstrap.input.model;

import android.content.Context;
import android.view.View;

import java.util.Arrays;

import git.artdeell.dnbootstrap.input.ControlButton;
import git.artdeell.dnbootstrap.input.LoadableButtonLayout;

public class ControlButtonData extends VisibilityConfiguration implements ViewCreator {
    public static final String TYPE = "button";
    public LoadableButtonLayout.LayoutParams layoutParams;
    public InputConfiguration inputConfiguration;
    public String label;
    public int[] keyCodes;
    public int backgroundColor;
    public String backgroundAssetId;

    public ControlButtonData() {}

    public ControlButtonData(ControlButtonData src) {
        this.layoutParams = new LoadableButtonLayout.LayoutParams(src.layoutParams);
        this.inputConfiguration = new InputConfiguration(src.inputConfiguration);
        this.label = src.label;
        this.keyCodes = Arrays.copyOf(src.keyCodes, src.keyCodes.length);
        this.showInGame = src.showInGame;
        this.showInMenu = src.showInMenu;
        this.backgroundColor = src.backgroundColor;
        this.backgroundAssetId = src.backgroundAssetId;
    }

    public static ControlButtonData createDefault() {
        ControlButtonData controlButtonData = new ControlButtonData();
        controlButtonData.keyCodes = new int[2];
        controlButtonData.label = "New";
        controlButtonData.showInGame = true;
        controlButtonData.showInMenu = true;
        controlButtonData.layoutParams = new LoadableButtonLayout.LayoutParams(6, 6);
        controlButtonData.layoutParams.offsetHorizontal = 6;
        controlButtonData.layoutParams.offsetVertical = 6;
        controlButtonData.inputConfiguration = new InputConfiguration();
        controlButtonData.inputConfiguration.sticky = true;
        controlButtonData.backgroundColor = 0;
        controlButtonData.backgroundAssetId = "";
        return controlButtonData;
    }

    @Override
    public View createView(Context context) {
        return new ControlButton(context, this);
    }

    @Override
    public String getType() {
        return ControlButtonData.TYPE;
    }
}
