package org.cacert.gigi.email;

import java.io.IOException;

public abstract class EmailProvider {
	public abstract void sendmail(String to, String subject, String message,
			String from, String replyto, String toname, String fromname,
			String errorsto, boolean extra) throws IOException;
	private static EmailProvider instance = new Sendmail();
	public static EmailProvider getInstance() {
		return instance;
	}
}
