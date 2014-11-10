package org.cacert.gigi.pages.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.error.PageNotFound;

public class TTPAdminPage extends Page {

    public static final String PATH = "/admin/ttp";

    public static final Group TTP_APPLICANT = Group.getByString("ttp-applicant");

    public TTPAdminPage() {
        super("TTP-Admin");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Form.getForm(req, TTPAdminForm.class).submit(resp.getWriter(), req);
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
        resp.sendRedirect(PATH);
    }

    private static final int PAGE_LEN = 30;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path != null && path.length() > PATH.length() + 1) {
            int id = Integer.parseInt(path.substring(1 + PATH.length()));
            User u = User.getById(id);
            if (u == null || !u.isInGroup(TTP_APPLICANT)) {
                req.setAttribute(PageNotFound.MESSAGE_ATTRIBUTE, "The TTP-request is not available anymore.");
                resp.sendError(404);
                return;
            }
            new TTPAdminForm(req, u).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
            return;
        }
        int offset = 0;
        String offsetS = req.getParameter("offset");
        if (offsetS != null) {
            offset = Integer.parseInt(offsetS);
        }

        final User[] users = TTP_APPLICANT.getMembers(offset, PAGE_LEN + 1);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("users", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i >= Math.min(PAGE_LEN, users.length)) {
                    return false;
                }
                vars.put("id", Integer.toString(users[i].getId()));
                vars.put("name", users[i].getName().toString());
                vars.put("email", users[i].getEmail());

                i++;
                return true;
            }
        });
        if (users.length == PAGE_LEN + 1) {
            vars.put("next", Integer.toString(offset + 30));
        }
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(User u) {
        return u != null && u.isInGroup(Group.getByString("ttp-assurer"));
    }
}
