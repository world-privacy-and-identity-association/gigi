package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;

public class MenuItem implements Outputable, IMenuItem {

    final String href;

    final String name;

    public MenuItem(String href, String name) {
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

}
