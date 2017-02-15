package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.util.AuthorizationContext;

public class MenuCollector implements IMenuItem {

    private LinkedList<Menu> items = new LinkedList<Menu>();

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        for (Menu menu : items) {
            menu.output(out, l, vars);
        }

    }

    @Override
    public boolean isPermitted(AuthorizationContext u) {
        return true;
    }

    public void put(Menu menu) {
        items.add(menu);
    }

}
