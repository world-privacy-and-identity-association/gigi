package club.wpia.gigi.pages.wot;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

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
        Map<String, Object> vars = getDefaultVars(req);
        vars.put("DoB", ds);
        t.output(out, getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canAssure();
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("search") == null) {
            AssuranceForm form = Form.getForm(req, AssuranceForm.class);
            return form.submitExceptionProtected(req, resp);
        }
        return super.beforePost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        if (req.getParameter("search") == null) {
            if (Form.printFormErrors(req, out)) {
                AssuranceForm form = Form.getForm(req, AssuranceForm.class);
                form.output(out, getLanguage(req), getDefaultVars(req));
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
                    out.println("Error, ambigous user. Please contact support.");
                } else {
                    if ( !verified) {
                        out.println(translate(req, "User is not yet verified. Please try again in 24 hours!"));
                    } else if (getUser(req).getId() == id) {

                    } else {
                        User assuree = User.getById(id);
                        try {
                            new AssuranceForm(req, assuree).output(out, getLanguage(req), getDefaultVars(req));
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
