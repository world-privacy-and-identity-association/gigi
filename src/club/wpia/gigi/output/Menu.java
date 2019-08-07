package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.util.AuthorizationContext;

public class Menu implements IMenuItem {

    public static final String AUTH_VALUE = "ac";

    private Outputable menuName;

    private IMenuItem[] content;

    private LinkedList<IMenuItem> prepare = new LinkedList<IMenuItem>();

    public Menu(Outputable menuName) {
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
                    out.print("<li class=\"dropdown\"><a href=\"#\" class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\" role=\"button\" aria-haspopup=\"true\" aria-expanded=\"false\">");
                    menuName.output(out, l, vars);
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

    public Outputable getMenuName() {
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
