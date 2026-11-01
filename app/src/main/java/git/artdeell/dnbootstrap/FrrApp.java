package git.artdeell.dnbootstrap;

import android.app.Application;

/** Runs before any Activity, so env is in place before GLFW/ANGLE/hostfxr initialise. */
public class FrrApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FrrRuntimeEnv.apply(this);
    }
}
