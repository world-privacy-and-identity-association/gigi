package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.AuthorizationContext;

public class SimpleMenuItem implements IMenuItem {

    private final String href;

    private final String name;

    public SimpleMenuItem(String href, String name) {
        this.href = href;
        this.name = name;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print("<li><a href=\"");
        out.print(href);
        out.print("\">");
        out.print(l.getTranslation(name));
        out.print("</a></li>");
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return true;
    }

}
