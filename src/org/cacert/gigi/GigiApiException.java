package org.cacert.gigi;

import java.io.PrintWriter;
import java.sql.SQLException;

public class GigiApiException extends Exception {
	SQLException e;
	String message;

	public GigiApiException(SQLException e) {
		this.e = e;
	}

	public GigiApiException(String message) {
		this.message = message;
	}

	public boolean isInternalError() {
		return e != null;
	}

	public void format(PrintWriter out, Language language) {
		if (isInternalError()) {
			e.printStackTrace();
			out.println(language.getTranslation("An internal error ouccured."));
		} else {
			out.println(language.getTranslation(message));
		}

	}

}
