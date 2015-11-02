package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.AuthorizationContext;

public class Menu implements IMenuItem {

    public static final String AUTH_VALUE = "ac";

    private String menuName;

    private IMenuItem[] content;

    private LinkedList<IMenuItem> prepare = new LinkedList<IMenuItem>();

    public Menu(String menuName) {
        this.menuName = menuName;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        boolean visible = false;
        AuthorizationContext u = (AuthorizationContext) vars.get(AUTH_VALUE);
        for (IMenuItem mi : content) {
            if (mi.isPermitted(u)) {
                if ( !visible) {
                    visible = true;
                    out.println("<div>");
                    out.print("<h3 class='pointer'>+ ");
                    out.print(l.getTranslation(menuName));
                    out.println("</h3>");
                    out.print("<ul class=\"menu\">");
                }
                mi.output(out, l, vars);
            }
        }
        if (visible) {
            out.println("</ul></div>");
        }
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
        return false;
    }

    @Override
    public int hashCode() {
        return menuName.hashCode();
    }

    public String getMenuName() {
        return menuName;
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return true;
    }
}
