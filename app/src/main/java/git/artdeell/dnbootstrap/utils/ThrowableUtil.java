package git.artdeell.dnbootstrap.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ThrowableUtil {
    public static String printStackTrace(Throwable t) {
        try(StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
            t.printStackTrace(printWriter);
            return stringWriter.toString();
        }catch (IOException e) {
            return "ThrowableUtil.printStackTrace() failed due to " + e;
        }
    }
}
