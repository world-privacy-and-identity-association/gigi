package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.localisation.Language;

public class Menu implements IMenuItem {

    public static final String USER_VALUE = "user";

    private String menuName;

    private String id;

    private IMenuItem[] content;

    private LinkedList<IMenuItem> prepare = new LinkedList<IMenuItem>();

    public Menu(String menuName, String id) {
        this.menuName = menuName;
        this.id = id;
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

    public void addItem(IMenuItem item) {
        prepare.add(item);
    }

    public void prepare() {
        content = new IMenuItem[prepare.size()];
        content = prepare.toArray(content);
        prepare = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Menu) {
            return menuName.equals(((Menu) obj).getMenuName());
        }
        return super.equals(obj);
    }

    public String getMenuName() {
        return menuName;
    }
}
