package org.cacert.gigi;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.LinkedList;

import org.cacert.gigi.localisation.Language;

public class GigiApiException extends Exception {

    private SQLException e;

    private LinkedList<String> messages = new LinkedList<>();

    public GigiApiException(SQLException e) {
        super(e);
        this.e = e;
    }

    public GigiApiException(String message) {
        super(message);
        messages.add(message);
    }

    public GigiApiException() {

    }

    public void mergeInto(GigiApiException e2) {
        messages.addAll(e2.messages);
        if (e == null) {
            e = e2.e;
        }
    }

    public boolean isInternalError() {
        return e != null;
    }

    public void format(PrintWriter out, Language language) {
        out.println("<div class='formError'>");
        if (isInternalError()) {
            e.printStackTrace();
            out.print("<div>");
            out.println(language.getTranslation("An internal error ouccured."));
            out.println("</div>");
        }
        for (String message : messages) {
            out.print("<div>");
            out.print(language.getTranslation(message));
            out.println("</div>");
        }
        out.println("</div>");

    }

    public boolean isEmpty() {
        return e == null && messages.size() == 0;
    }

}
