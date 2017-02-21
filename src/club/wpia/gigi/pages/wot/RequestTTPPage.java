package club.wpia.gigi.pages.wot;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Verification;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

public class RequestTTPPage extends Page {

    public static final String PATH = "/wot/ttp";

    public RequestTTPPage() {
        super("Request TTP");
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return Form.getForm(req, RequestTTPForm.class).submitExceptionProtected(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            Form.getForm(req, RequestTTPForm.class).output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = LoginPage.getUser(req);
        Map<String, Object> map = getDefaultVars(req);
        if (u.isInGroup(RequestTTPForm.TTP_APPLICANT)) {
            map.put("inProgress", true);
        } else {
            if (u.getVerificationPoints() < 100) {
                int ttpCount = 0;
                for (Verification a : u.getReceivedVerifications()) {
                    if (a.getMethod().equals(Verification.VerificationType.TTP_ASSISTED.getDescription())) {
                        ttpCount++;
                    }
                }
                if (ttpCount < 2) {
                    map.put("ttp", true);
                    map.put("form", new RequestTTPForm(req));
                } else {
                    map.put("nothing", true);
                }
            } else {
                map.put("nothing", true);
            }
        }
        map.put("form", new RequestTTPForm(req));
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), map);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.getTarget() instanceof User;
    }

}
