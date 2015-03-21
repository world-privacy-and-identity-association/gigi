package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

import sun.security.pkcs11.Secmod.DbMode;

public class SupportUserDetailsForm extends Form {

    private static Template t;

    private SupportedUser user;

    private DateSelector dobSelector;

    static {
        t = new Template(FindDomainForm.class.getResource("SupportUserDetailsForm.templ"));
    }

    public SupportUserDetailsForm(HttpServletRequest hsr, SupportedUser user) {
        super(hsr);
        this.user = user;
        dobSelector = new DateSelector("dobd", "dobm", "doby", user.getTargetUser().getDoB());
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        if (user.getTicket() == null) {
            return false;
        }
        dobSelector.update(req);
        String fname = req.getParameter("fname");
        String mname = req.getParameter("mname");
        String lname = req.getParameter("lname");
        String suffix = req.getParameter("suffix");
        if (fname == null || mname == null || lname == null | suffix == null) {
            throw new GigiApiException("Incomplete request!");
        }
        if ( !dobSelector.isValid()) {
            throw new GigiApiException("Invalid date of birth!");
        }
        Name newName = new Name(fname, lname, mname, suffix);
        if (user.setDob(dobSelector.getDate()) | user.setName(newName)) {
            user.submitSupportAction();
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        User user = this.user.getTargetUser();
        Name name = user.getName();
        vars.put("mail", user.getEmail());
        vars.put("fname", name.getFname());
        vars.put("mname", name.getMname());
        vars.put("lname", name.getLname());
        vars.put("suffix", name.getSuffix());
        vars.put("assurer", user.canAssure());
        vars.put("dob", dobSelector);
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
