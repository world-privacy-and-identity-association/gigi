package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class SupportUserDetailsForm extends Form {

    private static Template t;

    private User user;

    static {
        t = new Template(FindDomainForm.class.getResource("SupportUserDetailsForm.templ"));
    }

    public SupportUserDetailsForm(HttpServletRequest hsr, User user) {
        super(hsr);
        this.user = user;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("mail", user.getEmail());
        vars.put("fname", user.getFname());
        vars.put("mname", user.getMname());
        vars.put("lname", user.getLname());
        vars.put("suffix", user.getSuffix());
        vars.put("assurer", user.canAssure());
        vars.put("dob", new DateSelector("dobd", "dobm", "doby", user.getDob()));
        vars.put("blockedassurer", user.isInGroup(Group.BLOCKEDASSURER));
        vars.put("codesign", user.isInGroup(Group.CODESIGNING));
        vars.put("orgassurer", user.isInGroup(Group.ORGASSURER));
        vars.put("assurancepoints", user.getAssurancePoints());
        vars.put("blockedassuree", user.isInGroup(Group.BLOCKEDASSUREE));
        vars.put("ttpassurer", user.isInGroup(Group.TTP_ASSURER));
        vars.put("ttpapplicant", user.isInGroup(Group.TTP_APPLICANT));
        vars.put("blockedlogin", user.isInGroup(Group.BLOCKEDLOGIN));
        vars.put("supporter", user.isInGroup(Group.SUPPORTER));
        vars.put("id", user.getId());
        t.output(out, l, vars);
    }

}
