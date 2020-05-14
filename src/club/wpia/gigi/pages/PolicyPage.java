package club.wpia.gigi.pages;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.TimeConditions;

public class PolicyPage extends Page {

    public PolicyPage() {
        super("Policies");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> vars = Page.getDefaultVars(req);
        vars.put("appName", ServerConstants.getAppName());
        vars.put("testValidMonths", TimeConditions.getInstance().getTestMonths());
        vars.put("reverificationDays", TimeConditions.getInstance().getVerificationLimitDays());
        vars.put("verificationFreshMonths", TimeConditions.getInstance().getVerificationMonths());
        vars.put("verificationMaxAgeMonths", TimeConditions.getInstance().getVerificationMaxAgeMonths());
        vars.put("emailPingMonths", TimeConditions.getInstance().getEmailPingMonths());
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return true;
    }

}
