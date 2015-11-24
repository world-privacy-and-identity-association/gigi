package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.AssurancesDisplay;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class MyPoints extends Page {

    public static final String PATH = "/wot/mypoints";

    private AssurancesDisplay myDisplay = new AssurancesDisplay("asArr", false);

    private AssurancesDisplay toOtherDisplay = new AssurancesDisplay("otherAsArr", true);

    public MyPoints(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("pointlist", myDisplay);
        vars.put("madelist", toOtherDisplay);
        User user = getUser(req);
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
        return ac != null && ac.getTarget() instanceof User;
    }
}
