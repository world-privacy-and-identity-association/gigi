package org.cacert.gigi.pages.admin.support;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class SupportUserHistory extends Page {

    public static final String PATH = "/support/user/*/history";

    private static final int intStart = PATH.indexOf('*');

    public SupportUserHistory() {
        super("Support user history");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String info = req.getPathInfo();
        int id = Integer.parseInt(info.substring(intStart, info.length() - PATH.length() + intStart + 1));
        User u = User.getById(id);
        if (u == null) {
            resp.sendError(404);
            return;
        }
        // TODO get Admin log
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.isInGroup(Group.SUPPORTER);
    }
}
