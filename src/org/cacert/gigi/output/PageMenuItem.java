package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.ServerConstants;


public class PageMenuItem extends SimpleMenuItem {

    private Page p;

    public PageMenuItem(Page p) {
        super("https://" + ServerConstants.getWwwHostNamePort() + Gigi.getPathByPage(p), p.getTitle());
        this.p = p;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        if (p.isPermitted((User) vars.get(Menu.USER_VALUE))) {
            super.output(out, l, vars);
        }
    }
}
