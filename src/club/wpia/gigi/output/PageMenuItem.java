package club.wpia.gigi.output;

import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

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
