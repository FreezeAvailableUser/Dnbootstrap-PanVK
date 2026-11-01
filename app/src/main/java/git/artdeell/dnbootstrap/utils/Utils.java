package git.artdeell.dnbootstrap.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLConnection;

import git.artdeell.dnbootstrap.R;

public class Utils {
    public static <T> T getWeakReference(WeakReference<T> reference) {
        if(reference == null) return null;
        return reference.get();
    }

    public static void showErrorDialog(Activity activity, Throwable t, boolean exit) {
        activity.runOnUiThread(()-> new AlertDialog.Builder(activity)
                .setTitle(R.string.error)
                .setMessage(ThrowableUtil.printStackTrace(t))
                .setPositiveButton(android.R.string.ok, (d, v)-> {
                    if(exit) activity.finish();
                })
                .show());
    }

    /**
     * Determine the MIME type of a File.
     * @param file The file to determine the type of
     * @return the type, or the default value *slash* if cannot be determined
     */
    public static String getMimeType(File file) {
        if(file.isDirectory()) return DocumentsContract.Document.MIME_TYPE_DIR;
        String mimeType = null;
        try (FileInputStream fileInputStream = new FileInputStream(file)){
            // Theoretically we don't even need the buffer since we don't care about the
            // contents of the file after the guess, but mark-supported streams
            // are a requirement of URLConnection.guessContentTypeFromStream()
            try(BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                mimeType = URLConnection.guessContentTypeFromStream(bufferedInputStream);
            }
        }catch (IOException e) {
            Log.w("FileMimeType", "Failed to determine MIME type by stream", e);
        }
        if(mimeType != null) return mimeType;
        mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if(mimeType != null) return mimeType;
        return "*/*";
    }

    /**
     * Open the path specified by a File in a file explorer or in a relevant application.
     * @param context the current Context
     * @param file the File to open
     * @param share whether to open a "Share" or an "Open" dialog.
     */
    public static void openPath(Context context, File file, boolean share) {
        Uri contentUri = DocumentsContract.buildDocumentUri(context.getString(R.string.storageProviderAuthorities), file.getAbsolutePath());
        String mimeType = getMimeType(file);
        Intent intent = new Intent();
        if(share) {
            intent.setAction(Intent.ACTION_SEND);
            intent.setType(getMimeType(file));
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        }else {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent chooserIntent = Intent.createChooser(intent, file.getName());
        context.startActivity(chooserIntent);
    }
}
