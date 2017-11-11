package club.wpia.gigi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.TranslateCommand;

public class GigiApiException extends Exception {

    private static final Language PLAIN_LANGUAGE = Language.getInstance(Locale.ENGLISH);

    private static final long serialVersionUID = -164928670180852588L;

    private SQLException e;

    private LinkedList<Outputable> messages = new LinkedList<>();

    public GigiApiException(SQLException e) {
        super(e);
        this.e = e;
    }

    public GigiApiException(String message) {
        super(message);
        messages.add(new TranslateCommand(message));
    }

    public GigiApiException() {

    }

    public GigiApiException(Outputable out) {
        messages.add(out);
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

    public void format(PrintWriter out, Language language, Map<String, Object> vars) {
        out.println("<div class='alert alert-danger error-msgs'>");
        if (isInternalError()) {
            e.printStackTrace();
            out.print("<p>");
            out.println(language.getTranslation("An internal error occurred."));
            out.println("</p>");
        }
        for (Outputable message : messages) {
            HashMap<String, Object> map = new HashMap<>(vars);

            out.print("<p>");
            message.output(out, language, map);
            out.println("</p>");
        }
        out.println("</div>");

    }

    public void formatPlain(PrintWriter out) {
        if (isInternalError()) {
            out.println(PLAIN_LANGUAGE.getTranslation("An internal error occurred."));
        }
        HashMap<String, Object> map = new HashMap<>();
        for (Outputable message : messages) {
            if (message instanceof TranslateCommand) {
                String m = ((TranslateCommand) message).getRaw();
                // Skip HTML Entities
                out.println(PLAIN_LANGUAGE.getTranslation(m));
            } else {
                map.clear();
                message.output(out, PLAIN_LANGUAGE, map);
                out.println();
            }
        }
    }

    public boolean isEmpty() {
        return e == null && messages.size() == 0;
    }

    @Override
    public String getMessage() {
        if (messages.size() != 0) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            HashMap<String, Object> map = new HashMap<>();
            for (Outputable message : messages) {
                map.clear();
                message.output(pw, PLAIN_LANGUAGE, map);
            }
            pw.flush();

            return sw.toString();
        }
        return "";
    }

}
