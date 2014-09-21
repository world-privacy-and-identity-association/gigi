package org.cacert.gigi.pages.admin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.Page;

public class TTPAdminPage extends Page {

    public static final String PATH = "/admin/ttp";

    private static final Group TTP_APPLICANT = Group.getByString("ttp-applicant");

    public TTPAdminPage() {
        super("TTP-Admin");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path != null && path.length() > PATH.length()) {
            int id = Integer.parseInt(path.substring(1 + PATH.length()));
            resp.getWriter().println("processing: " + id); // TODO
            User u = User.getById(id);
            if ( !u.isInGroup(TTP_APPLICANT)) {
                return;
            }
            return;
        }
        final User[] users = TTP_APPLICANT.getMembers(0, 30);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("users", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i >= users.length) {
                    return false;
                }
                vars.put("id", Integer.toString(users[i].getId()));
                vars.put("name", users[i].getName().toString());
                vars.put("email", users[i].getEmail());

                i++;
                return true;
            }
        });
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(User u) {
        return u != null && u.isInGroup(Group.getByString("ttp-assuer"));
    }
}
