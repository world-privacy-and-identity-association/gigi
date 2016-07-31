package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class AssurePage extends Page {

    public static final String PATH = "/wot/assure";

    DateSelector ds = new DateSelector("day", "month", "year");

    private static final Template t = new Template(AssuranceForm.class.getResource("AssureeSearch.templ"));

    public AssurePage() {
        super("Verify someone");

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        PrintWriter out = resp.getWriter();
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("DoB", ds);
        t.output(out, getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canAssure();
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        if (req.getParameter("search") == null) {
            AssuranceForm form = Form.getForm(req, AssuranceForm.class);
            try {
                if (form.submit(out, req)) {
                    out.println(translate(req, "Verification complete."));
                    return;
                }
            } catch (GigiApiException e) {
                e.format(out, Page.getLanguage(req));
                form.output(out, getLanguage(req), new HashMap<String, Object>());
            }

            return;
        }

        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `users`.`id`, `verified` FROM `users` INNER JOIN `certOwners` ON `certOwners`.`id`=`users`.`id` WHERE `email`=? AND `dob`=? AND `deleted` IS NULL")) {
            ds.update(req);

            ps.setString(1, req.getParameter("email"));
            ps.setDate(2, ds.getDate().toSQLDate());
            GigiResultSet rs = ps.executeQuery();
            int id = 0;
            if (rs.next()) {
                id = rs.getInt(1);
                boolean verified = rs.getBoolean(2);
                if (rs.next()) {
                    out.println("Error, ambigous user. Please contact support@cacert.org.");
                } else {
                    if ( !verified) {
                        out.println(translate(req, "User is not yet verified. Please try again in 24 hours!"));
                    } else if (getUser(req).getId() == id) {

                    } else {
                        User assuree = User.getById(id);
                        try {
                            new AssuranceForm(req, assuree).output(out, getLanguage(req), new HashMap<String, Object>());
                        } catch (GigiApiException e) {
                            e.format(out, Page.getLanguage(req));
                        }
                    }
                }
            } else {
                throw new GigiApiException("I'm sorry, there was no email and date of birth matching" //
                        + " what you entered in the system. Please double check your information.");
            }

        } catch (GigiApiException e) {
            e.format(out, getLanguage(req));
        }
    }
}
