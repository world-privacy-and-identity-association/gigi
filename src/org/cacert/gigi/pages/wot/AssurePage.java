package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
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
        String pi = req.getPathInfo().substring(PATH.length());
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("DoB", ds);
        t.output(out, getLanguage(req), vars);
    }

    @Override
    public boolean isPermitted(User u) {
        try {
            return u != null && u.canAssure();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void outputForm(HttpServletRequest req, PrintWriter out, AssuranceForm form) {
        User myself = LoginPage.getUser(req);
        try {
            Notary.checkAssuranceIsPossible(myself, form.getAssuree());
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
        }

        form.output(out, getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        if (req.getParameter("search") == null) {
            AssuranceForm form = Form.getForm(req, AssuranceForm.class);
            if (form.submit(out, req)) {
                out.println(translate(req, "Assurance complete."));
            } else {
                outputForm(req, resp.getWriter(), form);
            }

            return;
        }

        ResultSet rs = null;
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id, verified FROM users WHERE email=? AND dob=? AND deleted=0");
            ps.setString(1, req.getParameter("email"));
            String day = req.getParameter("year") + "-" + req.getParameter("month") + "-" + req.getParameter("day");
            ps.setString(2, day);
            rs = ps.executeQuery();
            int id = 0;
            if (rs.next()) {
                id = rs.getInt(1);
                int verified = rs.getInt(2);
                if (rs.next()) {
                    out.println("Error, ambigous user. Please contact support@cacert.org.");
                } else {
                    if (verified == 0) {
                        out.println(translate(req, "User is not yet verified. Please try again in 24 hours!"));
                    } else if (getUser(req).getId() == id) {

                    } else {
                        AssuranceForm form = new AssuranceForm(req, id);
                        outputForm(req, out, form);
                    }
                }
            } else {
                out.print("<div class='formError'>");

                out.println(translate(req, "I'm sorry, there was no email and date of birth matching" + " what you entered in the system. Please double check" + " your information."));
                out.print("</div>");
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
