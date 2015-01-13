package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.Page;

public class SupportUserDetailsPage extends Page {

    public static final String PATH = "/support/user/";

    public SupportUserDetailsPage(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id;
        String[] idP = req.getPathInfo().split("/");
        id = Integer.parseInt(idP[idP.length - 1]);
        final User user = User.getById(id);
        SupportUserDetailsForm f = new SupportUserDetailsForm(req, user);
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("details", f);
        final EmailAddress[] addrs = user.getEmails();
        vars.put("emails", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                String address = addrs[i].getAddress();
                if ( !address.equals(user.getEmail())) {
                    vars.put("secmail", address);
                }
                i++;
                return i != addrs.length - 1;
            }
        });
        final Certificate[] certs = user.getCertificates(true);
        final CertificateProfile[] profiles = CertificateProfile.getAll();
        vars.put("types", new IterableDataset() {

            int typeIndex = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (typeIndex > profiles.length - 1) {
                    return false;
                }
                int valid = 0;
                int total = 0;
                long lastExpire = Long.MIN_VALUE;
                for (int i = 0; i < certs.length; i++) {
                    try {
                        if (certs[i].getProfile().getId() != profiles[typeIndex].getId()) {
                            continue;
                        }
                        total++;
                        certs[i].cert().checkValidity();
                        lastExpire = Math.max(lastExpire, certs[i].cert().getNotAfter().getTime());
                        valid++;
                    } catch (GeneralSecurityException | IOException e) {
                        continue;
                    }
                }
                vars.put("total", total);
                vars.put("profile", profiles[typeIndex].getVisibleName());
                vars.put("valid", valid);
                vars.put("exp", total - valid);
                vars.put("rev", "TODO");
                if (lastExpire == Long.MIN_VALUE) {
                    vars.put("lastdate", "-");
                } else {
                    vars.put("lastdate", DateSelector.getDateFormat().format(new Date(lastExpire)));
                }
                typeIndex++;
                return true;
            }
        });
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(User u) {
        if (u == null) {
            return false;
        }
        return u.isInGroup(Group.getByString("supporter"));
    }
}
