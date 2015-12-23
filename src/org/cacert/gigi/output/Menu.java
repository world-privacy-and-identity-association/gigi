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
        for (IMenuItem mi : getContent()) {
            if (mi.isPermitted(u)) {
                if ( !visible) {
                    visible = true;
                    out.print("<li class=\"dropdown\"><a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\" role=\"button\" aria-haspopup=\"true\" aria-expanded=\"false\">");
                    out.print(l.getTranslation(menuName));
                    out.print("<span class=\"caret\"></span></a><ul class=\"dropdown-menu\">");
                }
                mi.output(out, l, vars);
            }
        }
        if (visible) {
            out.println("</ul></li>");
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

    public IMenuItem[] getContent() {
        return content;
    }

}
