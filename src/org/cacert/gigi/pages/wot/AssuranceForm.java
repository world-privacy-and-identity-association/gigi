package org.cacert.gigi.pages.wot;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.Notary.AssuranceResult;

public class AssuranceForm extends Form {

    User assuree;

    static final Template templ;
    static {
        templ = new Template(AssuranceForm.class.getResource("AssuranceForm.templ"));
    }

    public AssuranceForm(HttpServletRequest hsr, int assuree) {
        super(hsr);
        this.assuree = new User(assuree);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        res.putAll(vars);
        res.put("name", assuree.getName());
        try {
            res.put("maxpoints", assuree.getMaxAssurePoints());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        res.put("dob", sdf.format(assuree.getDob()));
        templ.output(out, l, res);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        out.println("<div class='formError'>");
        boolean failed = false;

        if ( !"1".equals(req.getParameter("certify")) || !"1".equals(req.getParameter("rules")) || !"1".equals(req.getParameter("CCAAgreed")) || !"1".equals(req.getParameter("assertion"))) {
            outputError(out, req, "You failed to check all boxes to validate" + " your adherence to the rules and policies of CAcert");
            failed = true;

        }
        if (req.getParameter("date") == null || req.getParameter("date").equals("")) {
            outputError(out, req, "You must enter the date when you met the assuree.");
            failed = true;
        } else {
            try {
                Date d = sdf.parse(req.getParameter("date"));
                if (d.getTime() > System.currentTimeMillis()) {
                    outputError(out, req, "You must not enter a date in the future.");
                    failed = true;
                }
            } catch (ParseException e) {
                outputError(out, req, "You must enter the date in this format: YYYY-MM-DD.");
                failed = true;
            }
        }
        // check location, min 3 characters
        if (req.getParameter("location") == null || req.getParameter("location").equals("")) {
            outputError(out, req, "You failed to enter a location of your meeting.");
            failed = true;
        } else if (req.getParameter("location").length() <= 2) {
            outputError(out, req, "You must enter a location with at least 3 characters eg town and country.");
            failed = true;
        }
        // TODO checkPoints
        String points = req.getParameter("points");
        if (points == null || "".equals(points)) {
            // TODO message
            failed = true;
        }
        if (failed) {
            out.println("</div>");
            return false;
        }
        try {
            AssuranceResult success = Notary.assure(LoginPage.getUser(req), assuree, Integer.parseInt(req.getParameter("points")), req.getParameter("location"), req.getParameter("date"));
            if (success != AssuranceResult.ASSURANCE_SUCCEDED) {
                outputError(out, req, success.getMessage());
            }
            out.println("</div>");
            return success == AssuranceResult.ASSURANCE_SUCCEDED;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        out.println("</div>");
        return false;
    }

}
