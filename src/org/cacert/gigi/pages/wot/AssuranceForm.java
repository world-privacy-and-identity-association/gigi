package org.cacert.gigi.pages.wot;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.Notary;

public class AssuranceForm extends Form {

    private User assuree;

    private Name assureeName;

    private Date dob;

    private static final Template templ;
    static {
        templ = new Template(AssuranceForm.class.getResource("AssuranceForm.templ"));
    }

    public AssuranceForm(HttpServletRequest hsr, int assuree) {
        super(hsr);
        this.assuree = new User(assuree);
        assureeName = this.assuree.getName();
        dob = this.assuree.getDob();
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat sdf2 = new SimpleDateFormat("dd. MMM yyyy");

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        res.putAll(vars);
        res.put("nameExplicit", assuree.getName());
        res.put("name", assuree.getName().toString());
        try {
            res.put("maxpoints", assuree.getMaxAssurePoints());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        res.put("dob", sdf.format(assuree.getDob()));
        res.put("dobFmt2", sdf2.format(assuree.getDob()));
        templ.output(out, l, res);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        if ( !"1".equals(req.getParameter("certify")) || !"1".equals(req.getParameter("rules")) || !"1".equals(req.getParameter("CCAAgreed")) || !"1".equals(req.getParameter("assertion"))) {
            outputError(out, req, "You failed to check all boxes to validate" + " your adherence to the rules and policies of CAcert");

        }
        int pointsI = 0;
        String points = req.getParameter("points");
        if (points == null || "".equals(points)) {
            outputError(out, req, "For an assurance, you need to enter points.");
        } else {
            try {
                pointsI = Integer.parseInt(points);
            } catch (NumberFormatException e) {
                outputError(out, req, "The points entered were not a number.");
            }
        }

        if (isFailed(out)) {
            return false;
        }
        try {
            Notary.assure(Page.getUser(req), assuree, assureeName, dob, pointsI, req.getParameter("location"), req.getParameter("date"));
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
        }

        return false;
    }

    public User getAssuree() {
        return assuree;
    }

}
