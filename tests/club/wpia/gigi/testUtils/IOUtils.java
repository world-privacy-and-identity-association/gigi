package club.wpia.gigi.testUtils;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URLConnection;

public class IOUtils {

    private IOUtils() {

    }

    public static String readURL(URLConnection in) {
        try {
            if ( !in.getContentType().equals("text/html; charset=UTF-8") && !in.getContentType().equals("text/plain; charset=UTF-8") && !in.getContentType().equals("application/json; charset=UTF-8")) {
                if (in instanceof HttpURLConnection && ((HttpURLConnection) in).getResponseCode() != 200) {
                    System.err.println(readURL(new InputStreamReader(((HttpURLConnection) in).getErrorStream(), "UTF-8")));
                }
                throw new Error("Unrecognized content-type: " + in.getContentType());
            }
            return readURL(new InputStreamReader(in.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            throw new Error(e);
        }

    }

    public static String readURL(Reader in) {
        CharArrayWriter caw = new CharArrayWriter();
        char[] buffer = new char[1024];
        int len = 0;
        try {
            while ((len = in.read(buffer)) > 0) {
                caw.write(buffer, 0, len);
            }
            in.close();
            return new String(caw.toCharArray());
        } catch (IOException e) {
            throw new Error(e);
        }

    }

    public static byte[] readURL(InputStream in) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        try {
            while ((len = in.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            in.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
