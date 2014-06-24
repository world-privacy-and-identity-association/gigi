package org.cacert.gigi.email;

import java.io.IOException;
import java.util.Properties;

public abstract class EmailProvider {
	public abstract void sendmail(String to, String subject, String message,
			String from, String replyto, String toname, String fromname,
			String errorsto, boolean extra) throws IOException;
	private static EmailProvider instance;
	public static EmailProvider getInstance() {
		return instance;
	}
	public static void init(Properties conf) {
		try {
			Class<?> c = Class.forName(conf.getProperty("emailProvider"));
			instance = (EmailProvider) c.getDeclaredConstructor(
					Properties.class).newInstance(conf);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}
}
