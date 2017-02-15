package club.wpia.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;

public class FindAgentAccess extends Form {

    private User target;

    public FindAgentAccess(HttpServletRequest hsr) {
        super(hsr);
        target = LoginPage.getUser(hsr);
    }

    public static final String PATH = "/account/find-agent";

    private static final Template t = new Template(ChangePasswordPage.class.getResource("FindAgentAccess.templ"));

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String nv = req.getParameter("new-val");
        if (nv == null) {
            throw new GigiApiException("Parameter new-val missing.");
        }
        if (nv.equals("enable")) {
            target.grantGroup(target, Group.LOCATE_AGENT);
        } else {
            target.revokeGroup(target, Group.LOCATE_AGENT);
        }
        return new RedirectResult(FindAgentAccess.PATH);
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        boolean inGroup = target.isInGroup(Group.LOCATE_AGENT);
        vars.put("enable", inGroup ? " disabled" : "");
        vars.put("disable", !inGroup ? " disabled" : "");
        t.output(out, l, vars);
    }

}
