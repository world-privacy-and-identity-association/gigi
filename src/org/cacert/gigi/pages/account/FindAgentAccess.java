package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

public class FindAgentAccess extends Form {

    private User target;

    public FindAgentAccess(HttpServletRequest hsr) {
        super(hsr);
        target = LoginPage.getUser(hsr);
    }

    public static final String PATH = "/account/find-agent";

    private static final Template t = new Template(ChangePasswordPage.class.getResource("FindAgentAccess.templ"));

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        String nv = req.getParameter("new-val");
        if (nv == null) {
            return false;
        }
        if (nv.equals("enable")) {
            target.grantGroup(target, Group.LOCATE_AGENT);
        } else {
            target.revokeGroup(target, Group.LOCATE_AGENT);
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        boolean inGroup = target.isInGroup(Group.LOCATE_AGENT);
        vars.put("enable", inGroup ? " disabled" : "");
        vars.put("disable", !inGroup ? " disabled" : "");
        t.output(out, l, vars);
    }

}
