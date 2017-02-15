package club.wpia.gigi.pages.wot;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.output.AssurancesDisplay;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

public class Points extends Page {

    public static final String SUPPORT_PATH = "/support/user/*/points";

    public static final String PATH = "/wot/points";

    private static final int intStart = SUPPORT_PATH.indexOf('*');

    private boolean support;

    private AssurancesDisplay myDisplay;

    private AssurancesDisplay toOtherDisplay;

    public Points(boolean support) {
        super(support ? "Support User Points" : "Points");
        this.support = support;
        myDisplay = new AssurancesDisplay("asArr", false, support);
        toOtherDisplay = new AssurancesDisplay("otherAsArr", true, support);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user;
        if (support) {
            String info = req.getPathInfo();
            int id = Integer.parseInt(info.substring(intStart, info.length() - SUPPORT_PATH.length() + intStart + 1));
            user = User.getById(id);
            if (user == null) {
                resp.sendError(404);
                return;
            }
        } else {
            user = getUser(req);
        }

        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("support", support);
        vars.put("username", user.getPreferredName().toString());
        vars.put("pointlist", myDisplay);
        vars.put("madelist", toOtherDisplay);
        vars.put("asArr", user.getReceivedAssurances());
        vars.put("otherAsArr", user.getMadeAssurances());
        vars.put("assP", user.getAssurancePoints());
        if (user.canAssure()) {
            vars.put("expP", user.getExperiencePoints());
            vars.put("maxP", user.getMaxAssurePoints());
        }
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
