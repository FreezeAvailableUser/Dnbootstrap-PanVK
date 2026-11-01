package git.artdeell.dnbootstrap.assets;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import git.artdeell.dnbootstrap.utils.Utils;
import git.artdeell.dnbootstrap.io.IOUtil;
import git.artdeell.dnbootstrap.io.ReadCountInputStream;

public class AssetsExtractor implements Runnable, ReadCountInputStream.Callback {
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private long totalRead;
    private WeakReference<ProgressCallback> callback;
    private final Context appContext;
    private final Uri gameUri;
    private long totalSize;

    public AssetsExtractor(Context context, Uri gameUri, long gameSize) {
        this.appContext = context;
        this.gameUri = gameUri;
        this.totalSize = gameSize;
    }

    public void setCallback(ProgressCallback activity) {
        this.callback = new WeakReference<>(activity);
    }

    @Override
    public void run() {
        try {
            runCatching();
        }catch (Exception e) {
            Log.i("AssetsExtractor", "Extraction failed", e);
        }
    }

    private void extractGame(AppDirs appDirs) throws IOException {
        try(InputStream inputStream = appContext.getContentResolver().openInputStream(gameUri)) {
            InputStream fileInputStream = new ReadCountInputStream(inputStream, this);
            IOUtil.extractTarGzFile(fileInputStream, appDirs.vs);
            IOUtil.markComponentInstalled(appDirs.vs);
        }
    }

    private void extractAppComponent(AssetManager assetManager, String assetName, File outDir) throws Exception {
        try(InputStream assetStream = assetManager.open(assetName)) {
            InputStream fileInputStream = new ReadCountInputStream(assetStream, this);
            IOUtil.extractTarGzFile(fileInputStream, outDir);
            IOUtil.markComponentInstalled(outDir);
        }
    }

    private void runCatching() throws Exception {
        AppDirs appDirs = new AppDirs(appContext.getFilesDir());
        AssetManager assetManager = appContext.getAssets();
        if(gameUri != null) FileUtils.deleteDirectory(appDirs.vs);
        FileUtils.deleteDirectory(appDirs.runtime);
        FileUtils.deleteDirectory(appDirs.fontconfig);
        if(totalSize != -1) totalSize += 22673999 + 1137044; // TODO remove hardcoded size
        if(gameUri != null) extractGame(appDirs);
        extractAppComponent(assetManager, "dotnet-runtime.tgz", appDirs.runtime);
        extractAppComponent(assetManager, "fontconfig.tgz", appDirs.fontconfig);
        complete.set(true);
        callProgressCallback();
    }

    public double getProgress() {
        return (double) totalRead / totalSize;
    }

    public boolean progressAvailable() {
        return totalSize != -1;
    }

    public boolean progressComplete() {
        return complete.get();
    }

    @Override
    public void updateBytesRead(long bytesRead) {
        if(totalSize == -1) return;
        totalRead += bytesRead;
        callProgressCallback();
    }

    private void callProgressCallback() {
        ProgressCallback callback = Utils.getWeakReference(this.callback);
        callback.onProgressChanged();
    }

    public interface ProgressCallback {
        void onProgressChanged();
    }
}
