package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.Language;

public class MailTable implements Outputable {
	private String resultSet, userMail;

	public MailTable(String key, String userMail) {
		this.resultSet = key;
		this.userMail = userMail;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {

	}

}
