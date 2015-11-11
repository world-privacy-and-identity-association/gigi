package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.OutputableArrayIterable;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class History extends Page {

    public static final String SUPPORT_PATH = "/support/user/*/history";

    public static final String PATH = "/account/history";

    private static final int intStart = SUPPORT_PATH.indexOf('*');

    private boolean support;

    public History(boolean support) {
        super(support ? "Support History" : "History");
        this.support = support;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CertificateOwner u;
        if (support) {
            String info = req.getPathInfo();
            int id = Integer.parseInt(info.substring(intStart, info.length() - SUPPORT_PATH.length() + intStart + 1));
            u = User.getById(id);
            if (u == null) {
                resp.sendError(404);
                return;
            }
        } else {
            u = LoginPage.getAuthorizationContext(req).getTarget();
        }
        String[] adminLog = u.getAdminLog();
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("entries", new OutputableArrayIterable(adminLog, "entry"));
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ( !support || ac.canSupport());
    }
}
