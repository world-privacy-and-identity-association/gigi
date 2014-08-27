package org.cacert.gigi.output;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.User;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.ServerConstants;


public class PageMenuItem extends SimpleMenuItem {

    private Page p;

    public PageMenuItem(Page p) {
        super("https://" + ServerConstants.getWwwHostNamePortSecure() + Gigi.getPathByPage(p), p.getTitle());
        this.p = p;
    }

    @Override
    public boolean isPermitted(User u) {
        return p.isPermitted(u);
    }
}
