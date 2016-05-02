package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.Sendmail;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.GroupSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.PasswordResetPage;
import org.cacert.gigi.util.RandomToken;
import org.cacert.gigi.util.ServerConstants;

public class SupportUserDetailsForm extends Form {

    private static Template t;

    private SupportedUser user;

    private DateSelector dobSelector;

    private GroupSelector value = new GroupSelector("groupToModify");

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
        if ((req.getParameter("detailupdate") != null ? 1 : 0) + (req.getParameter("grant") != null ? 1 : 0) + (req.getParameter("deny") != null ? 1 : 0) + (req.getParameter("resetPass") != null ? 1 : 0) != 1) {
            throw new GigiApiException("More than one action requested!");
        }
        if (req.getParameter("grant") != null || req.getParameter("deny") != null) {
            value.update(req);
            Group toMod = value.getGroup();
            if (req.getParameter("grant") != null) {
                user.grant(toMod);
            } else {
                user.revoke(toMod);
            }
            return true;
        }
        if (req.getParameter("resetPass") != null) {
            String aword = req.getParameter("aword");
            if (aword == null || aword.equals("")) {
                throw new GigiApiException("An A-Word is required to perform a password reset.");
            }
            String ptok = RandomToken.generateToken(32);
            int id = user.getTargetUser().generatePasswordResetTicket(Page.getUser(req), ptok, aword);
            try {
                Language l = Language.getInstance(user.getTargetUser().getPreferredLocale());
                StringBuffer body = new StringBuffer();
                body.append(l.getTranslation("Hi,") + "\n\n");
                body.append(l.getTranslation("A password reset was triggered. Please enter the required text sent to you by support on this page: \nhttps://"));
                body.append(ServerConstants.getWwwHostNamePortSecure() + PasswordResetPage.PATH);
                body.append("?id=");
                body.append(id);
                body.append("&token=");
                body.append(URLEncoder.encode(ptok, "UTF-8"));
                body.append("\n");
                body.append("\n");
                body.append(l.getTranslation("Best regards"));
                body.append("\n");
                body.append(l.getTranslation("CAcert.org Support!"));
                Sendmail.getInstance().sendmail(user.getTargetUser().getEmail(), "[CAcert.org] " + l.getTranslation("Password reset by support."), body.toString(), "support@cacert.org", null, null, null, null, false);
                out.println(Page.getLanguage(req).getTranslation("Password reset successful."));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
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
        synchronized (user.getTargetUser()) {
            if (user.setDob(dobSelector.getDate()) | user.setName(newName)) {
                user.submitSupportAction();
            }
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
        vars.put("assurancepoints", user.getAssurancePoints());
        vars.put("exppoints", user.getExperiencePoints());
        vars.put("id", user.getId());
        final Set<Group> gr = user.getGroups();
        vars.put("groups", new IterableDataset() {

            Iterator<Group> i = gr.iterator();

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if ( !i.hasNext()) {
                    return false;
                }
                Group g = i.next();
                vars.put("group_name", l.getTranslation("Group: " + g.getDatabaseName()));
                return true;
            }
        });
        vars.put("groupSelector", value);
        t.output(out, l, vars);
    }

}
