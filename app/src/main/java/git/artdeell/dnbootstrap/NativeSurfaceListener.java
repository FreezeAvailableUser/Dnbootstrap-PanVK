package git.artdeell.dnbootstrap;

import android.view.Surface;
import android.view.SurfaceHolder;

public class NativeSurfaceListener implements SurfaceHolder.Callback {

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        nativeSurfaceCreated(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        nativeSurfaceDestroyed();
    }

    private static native void nativeSurfaceCreated(Surface surface);
    private static native void nativeSurfaceDestroyed();
}
