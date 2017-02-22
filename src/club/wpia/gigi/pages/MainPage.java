package club.wpia.gigi.pages;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.GroupIterator;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;

public class MainPage extends Page {

    private static final Template notLog = new Template(MainPage.class.getResource("MainPageNotLogin.templ"));

    public MainPage() {
        super("Home");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> vars = getDefaultVars(req);
        if (LoginPage.getUser(req) != null) {
            User u = LoginPage.getUser(req);
            vars.put("username", u.getPreferredName());
            final Set<Group> gr = u.getGroups();
            vars.put("support-groups", new GroupIterator(gr.iterator(), true));
            vars.put("groups", new GroupIterator(gr.iterator(), false));
            vars.put("ra-agent", u.canVerify());
            vars.put("vp", u.getVerificationPoints());
            vars.put("xp", u.getExperiencePoints());

            Certificate[] c = u.getCertificates(false);
            vars.put("c-no", c.length);

            final EmailAddress[] emails = u.getEmails();
            IterableDataset ds = new IterableDataset() {

                private int point = 0;

                @Override
                public boolean next(Language l, Map<String, Object> vars) {
                    if (point >= emails.length) {
                        return false;
                    }
                    EmailAddress emailAddress = emails[point];
                    vars.put("verification", l.getTranslation(emailAddress.isVerified() ? "Verified" : "Unverified"));
                    vars.put("last_verification", emailAddress.getLastPing(true));
                    vars.put("address", emailAddress.getAddress());
                    point++;
                    return true;
                }
            };
            vars.put("emails", ds);

            final Domain[] doms = u.getDomains();
            IterableDataset dts = new IterableDataset() {

                private int point = 0;

                @Override
                public boolean next(Language l, Map<String, Object> vars) {
                    if (point >= doms.length) {
                        return false;
                    }
                    Domain domain = doms[point];
                    vars.put("domain", domain.getSuffix());
                    vars.put("status", l.getTranslation(domain.isVerified() ? "Verified" : "Unverified"));
                    point++;
                    return true;
                }
            };
            vars.put("domains", dts);
            vars.put("nodomains", doms.length == 0);

            final List<Organisation> o = u.getOrganisations();
            vars.put("orgas", new IterableDataset() {

                Iterator<Organisation> it = o.iterator();

                @Override
                public boolean next(Language l, Map<String, Object> vars) {
                    if ( !it.hasNext()) {
                        return false;
                    }
                    Organisation o = it.next();
                    vars.put("orgName", o.getName());
                    vars.put("orgID", o.getId());
                    return true;
                }
            });
            vars.put("hasorgs", !o.isEmpty());
            getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);

        } else {
            notLog.output(resp.getWriter(), getLanguage(req), vars);
        }
    }

    @Override
    public boolean needsLogin() {
        return false;
    }
}
