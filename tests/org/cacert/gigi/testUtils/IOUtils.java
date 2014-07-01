package org.cacert.gigi.testUtils;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;

public class IOUtils {
	private IOUtils() {

	}
	public static String readURL(URLConnection in) {
		try {
			if (!in.getContentType().equals("text/html; charset=UTF-8")) {
				throw new Error("Unrecognized content-type: "
						+ in.getContentType());
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
			return new String(caw.toCharArray());
		} catch (IOException e) {
			throw new Error(e);
		}

	}
}
