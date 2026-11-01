package git.artdeell.dnbootstrap.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadCountInputStream extends FilterInputStream {
    private final Callback callback;
    private long readBytes;
    public ReadCountInputStream(InputStream in, Callback callback) {
        super(in);
        this.callback = callback;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int rc = super.read(b);
        if(rc != -1) {
            readBytes += rc;
            updateBytesRead();
        }
        return rc;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int rc = super.read(b, off, len);
        if(rc != -1) {
            readBytes += rc;
            updateBytesRead();
        }
        return rc;
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if(result != -1) {
            readBytes++;
            updateBytesRead();
        }
        return result;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new RuntimeException("Not supported");
    }

    private void updateBytesRead() {
        if(readBytes < 1024 * 1024) return;
        callback.updateBytesRead(readBytes);
        readBytes = 0;
    }

    public interface Callback {
        void updateBytesRead(long bytesRead);
    }
}
