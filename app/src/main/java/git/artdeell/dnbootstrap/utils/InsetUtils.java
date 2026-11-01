package git.artdeell.dnbootstrap.utils;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

public class InsetUtils {
    @RequiresApi(Build.VERSION_CODES.P)
    private static void setCutoutMode(Window window, boolean ignoreNotch) {
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (ignoreNotch) {
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        } else {
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    }

    @SuppressWarnings("deprecation")
    private static void setLegacyFullscreen(View insetView, boolean fullscreen) {
        View.OnSystemUiVisibilityChangeListener listener = (visibility)->{
            if(fullscreen && (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                insetView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }else if(!fullscreen) {
                insetView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        };
        listener.onSystemUiVisibilityChange(insetView.getSystemUiVisibility());
        insetView.setOnSystemUiVisibilityChangeListener(listener);
    }

    public static void setInsetsMode(Activity activity, boolean noSystemBars, boolean ignoreNotch) {
        Window window = activity.getWindow();
        View insetView = activity.findViewById(android.R.id.content);
        // Don't ignore system bars in window mode (will put game behind window button bar)
        if(SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode()) noSystemBars = false;

        int bgColor = Color.BLACK;

        // On API 35 onwards, apps are edge-to-edge by default and are controlled entirely though the
        // inset API. On levels below, we still need to set the correct cutout mode.
        if(SDK_INT >= Build.VERSION_CODES.P) setCutoutMode(window, ignoreNotch);

        // The AppCompat APIs don't work well, and break when opening alert dialogs on older Android
        // versions. Use the legacy fullscreen flags for lower APIs. (notch is already handled above)
        if(SDK_INT < Build.VERSION_CODES.R) {
            setLegacyFullscreen(insetView, noSystemBars);
            return;
        }
        // Code below expects this to be set to false, since that's the SDK 35 default.
        if(SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setDecorFitsSystemWindows(false);
        }

        WindowInsetsController insetsController = window.getInsetsController();
        if(insetsController != null) {
            insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            if(noSystemBars) insetsController.hide(WindowInsets.Type.systemBars());
            else insetsController.show(WindowInsets.Type.systemBars());
        }

        boolean fFullscreen = noSystemBars;
        insetView.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            int insetMask = 0;
            if(!fFullscreen) insetMask |= WindowInsets.Type.systemBars();
            if(!ignoreNotch) insetMask |= WindowInsets.Type.displayCutout();
            if(insetMask != 0) {
                Insets insets = windowInsets.getInsets(insetMask);
                v.setBackground(new InsetBackground(insets,bgColor));
                insetView.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }else {
                insetView.setPadding(0, 0, 0, 0);
                v.setBackground(null);
            }
            return WindowInsets.CONSUMED;
        });
        insetView.requestApplyInsets();
    }
}
