package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.Calendar;
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
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.Notary;

public class AssurePage extends Page {

    public static final String PATH = "/wot/assure";

    DateSelector ds = new DateSelector("day", "month", "year");

    Template t;

    public AssurePage() {
        super("Assure someone");
        t = new Template(AssuranceForm.class.getResource("AssureeSearch.templ"));

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
            if (form.submit(out, req)) {
                out.println(translate(req, "Assurance complete."));
            } else {
                try {
                    Notary.checkAssuranceIsPossible(LoginPage.getUser(req), form.getAssuree());
                    form.output(out, getLanguage(req), new HashMap<String, Object>());
                } catch (GigiApiException e) {
                    e.format(out, Page.getLanguage(req));
                }
            }

            return;
        }

        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `users`.`id`, `verified` FROM `users` INNER JOIN `certOwners` ON `certOwners`.`id`=`users`.`id` WHERE `email`=? AND `dob`=? AND `deleted` IS NULL")) {
            ps.setString(1, req.getParameter("email"));
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(req.getParameter("year")), Integer.parseInt(req.getParameter("month")) - 1, Integer.parseInt(req.getParameter("day")));
            ps.setDate(2, new Date(c.getTimeInMillis()));
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
                        User myself = LoginPage.getUser(req);
                        try {
                            Notary.checkAssuranceIsPossible(myself, assuree);
                            new AssuranceForm(req, assuree).output(out, getLanguage(req), new HashMap<String, Object>());
                        } catch (GigiApiException e) {
                            e.format(out, Page.getLanguage(req));
                        }
                    }
                }
            } else {
                out.print("<div class='formError'>");

                out.println(translate(req, "I'm sorry, there was no email and date of birth matching" + " what you entered in the system. Please double check" + " your information."));
                out.print("</div>");
            }

        }
    }
}
