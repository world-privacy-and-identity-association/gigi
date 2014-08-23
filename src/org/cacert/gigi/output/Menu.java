package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;

public class Menu implements Outputable {

    String menuName;

    String id;

    private IMenuItem[] content;

    public Menu(String menuName, String id, IMenuItem... content) {
        this.menuName = menuName;
        this.id = id;
        this.content = content;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<div>");
        out.print("<h3>+ ");
        out.print(l.getTranslation(menuName));
        out.print("</h3>");
        out.print("<ul class=\"menu\" id=\"");
        out.print(id);
        out.print("\">");
        for (Outputable mi : content) {
            mi.output(out, l, vars);
        }

        out.println("</ul></div>");
    }
}
