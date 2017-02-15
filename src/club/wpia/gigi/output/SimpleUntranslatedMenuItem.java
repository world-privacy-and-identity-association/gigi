package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.util.AuthorizationContext;

public class SimpleUntranslatedMenuItem implements IMenuItem {

    private final String href;

    protected final String name;

    public SimpleUntranslatedMenuItem(String href, String name) {
        this.href = href;
        this.name = name;
    }

    protected void printContent(PrintWriter out, Language l) {
        out.print(name);
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print("<li><a href=\"");
        out.print(href);
        out.print("\">");
        printContent(out, l);
        out.print("</a></li>");
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return true;
    }

}
