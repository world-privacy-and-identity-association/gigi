package club.wpia.gigi.pages.admin;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.SprintfCommand;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.pages.error.PageNotFound;
import club.wpia.gigi.util.AuthorizationContext;

public class TTPAdminPage extends Page {

    public static final String PATH = "/admin/ttp";

    public static final Group TTP_APPLICANT = Group.TTP_APPLICANT;

    public TTPAdminPage() {
        super("TTP-Admin");
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return Form.getForm(req, TTPAdminForm.class).submitExceptionProtected(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            Form.getForm(req, TTPAdminForm.class).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        }
    }

    private static final int PAGE_LEN = 30;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path != null && path.length() > PATH.length() + 1) {
            int id = Integer.parseInt(path.substring(1 + PATH.length()));
            User u = User.getById(id);
            if (u == null || !u.isInGroup(TTP_APPLICANT)) {
                SprintfCommand command = new SprintfCommand("The TTP-request is not available anymore. You might want to go {0}back{1}.", Arrays.asList("!'<a href=\"" + PATH + "\">", "!'</a>"));
                req.setAttribute(PageNotFound.MESSAGE_ATTRIBUTE, command);
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
                vars.put("name", users[i].getPreferredName().toString());
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
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.isInGroup(Group.TTP_AGENT);
    }
}
