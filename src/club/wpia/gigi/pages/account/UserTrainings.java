package club.wpia.gigi.pages.account;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.output.template.OutputableArrayIterable;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

public class UserTrainings extends Page {

    public static final String SUPPORT_PATH = "/support/user/*/trainings";

    public static final String PATH = "/account/trainings";

    private static final int intStart = SUPPORT_PATH.indexOf('*');

    private boolean support;

    public UserTrainings(boolean support) {
        super(support ? "Support User Trainings" : "Trainings");
        this.support = support;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u;
        HashMap<String, Object> vars = new HashMap<>();
        if (support) {
            String info = req.getPathInfo();
            int id = Integer.parseInt(info.substring(intStart, info.length() - SUPPORT_PATH.length() + intStart + 1));
            u = User.getById(id);
            if (u == null) {
                resp.sendError(404);
                return;
            }
            vars.put("username", u.getPreferredName().toString());
        } else {
            u = getUser(req);
        }
        vars.put("entries", new OutputableArrayIterable(u.getTrainings(), "entry"));
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        if (ac == null) {
            return false;
        }
        if (support) {
            return ac.canSupport();
        } else {
            return ac.getTarget() instanceof User;
        }
    }
}
