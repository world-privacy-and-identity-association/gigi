package org.cacert.gigi.output;

import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class PageMenuItem extends SimpleMenuItem {

    private Page p;

    public PageMenuItem(Page p, String path) {
        // "https://" + ServerConstants.getWwwHostNamePortSecure() +
        super(path, p.getTitle());
        this.p = p;
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return p.isPermitted(ac);
    }
}
