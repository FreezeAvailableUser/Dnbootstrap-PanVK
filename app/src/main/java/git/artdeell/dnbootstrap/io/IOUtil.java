package git.artdeell.dnbootstrap.io;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class IOUtil {
    public static void tryMkdirs(File dir) throws IOException {
        if(dir.isDirectory()) return;
        if(!dir.mkdirs()) throw new IOException("Failed to create directory "+dir.getName());
    }

    public static void write(InputStream inputStream, File output, byte[] buffer) throws IOException {
        try(FileOutputStream fileOutputStream = new FileOutputStream(output)) {
            int rc;
            while ((rc = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, rc);
            }
        }
    }

    public static void extractTarGzFile(InputStream inputStream, File targetDir) throws IOException {
        byte[] buffer = new byte[65535];
        try(GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
            TarArchiveInputStream tarStream = new TarArchiveInputStream(gzipInputStream)) {
            TarArchiveEntry entry;
            while((entry = tarStream.getNextEntry()) != null) {
                File destination = new File(targetDir, entry.getName());
                if(entry.isDirectory()) tryMkdirs(destination);
                else {
                    tryMkdirs(Objects.requireNonNull(destination.getParentFile()));
                    write(tarStream, destination, buffer);
                }
            }
        }
    }

    public static boolean checkComponentInstalled(File componentDir) {
        return new File(componentDir, ".installed").exists();
    }

    public static void markComponentInstalled(File componentDir) throws IOException {
        File markFile = new File(componentDir, ".installed");
        if(markFile.exists()) return;
        if(!markFile.createNewFile()) throw new IOException("Failed to mark "+componentDir.getName()+ " as installed");
    }

    public static long getFileSize(ContentResolver contentResolver, Uri uri) {
        try(Cursor cursor = contentResolver.query(uri, null, null, null, null, null)) {
            if(cursor == null) return -1;
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if(sizeIndex == -1) return -1;
            if(!cursor.moveToFirst()) return -1;
            return cursor.getLong(sizeIndex);
        }
    }
}
