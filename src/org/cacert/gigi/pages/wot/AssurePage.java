package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.Notary.AssuranceResult;

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
        if (pi.length() > 1) {
            int mid = Integer.parseInt(pi.substring(1));
            AssuranceForm form = new AssuranceForm(req, mid);
            outputForm(req, out, mid, form);

        } else {
            HashMap<String, Object> vars = new HashMap<String, Object>();
            vars.put("DoB", ds);
            t.output(out, getLanguage(req), vars);
        }
    }

    private void outputForm(HttpServletRequest req, PrintWriter out, int mid, AssuranceForm form) {
        User myself = LoginPage.getUser(req);
        AssuranceResult check = Notary.checkAssuranceIsPossible(myself, new User(mid));
        if (check != AssuranceResult.ASSURANCE_SUCCEDED) {
            out.println(translate(req, check.getMessage()));
            return;
        }
        if (form == null || form.getAssuree().getId() != mid) {
            form = new AssuranceForm(req, mid);
        }

        form.output(out, getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() > 1) {
            User myself = getUser(req);
            int mid = Integer.parseInt(pi.substring(1));
            if (mid == myself.getId()) {
                out.println(translate(req, "Cannot assure myself."));
                return;
            }

            AssuranceForm form = Form.getForm(req, AssuranceForm.class);
            if (mid != form.getAssuree().getId()) {
                return;
            }
            if (form.submit(out, req)) {
                out.println(translate(req, "Assurance complete."));
            } else {
                outputForm(req, resp.getWriter(), mid, form);
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
                    }
                    resp.sendRedirect(PATH + "/" + id);
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
