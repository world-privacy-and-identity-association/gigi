package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.Sendmail;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.PasswordResetPage;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.RandomToken;
import org.cacert.gigi.util.ServerConstants;

public class AssuranceForm extends Form {

    private User assuree;

    private Name assureeName;

    private DayDate dob;

    private String location = "";

    private String date = "";

    private String aword;

    private User assurer;

    private AssuranceType type = AssuranceType.FACE_TO_FACE;

    private static final Template templ;
    static {
        templ = new Template(AssuranceForm.class.getResource("AssuranceForm.templ"));
    }

    public AssuranceForm(HttpServletRequest hsr, User assuree) {
        super(hsr);
        assurer = Page.getUser(hsr);
        this.assuree = assuree;
        assureeName = this.assuree.getName();
        dob = this.assuree.getDoB();
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat sdf2 = new SimpleDateFormat("dd. MMM yyyy");

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        res.putAll(vars);
        res.put("nameExplicit", assuree.getName());
        res.put("name", assuree.getName().toString());
        res.put("maxpoints", assurer.getMaxAssurePoints());
        res.put("dob", sdf.format(assuree.getDoB().toDate()));
        res.put("dobFmt2", sdf2.format(assuree.getDoB().toDate()));
        res.put("location", location);
        res.put("date", date);
        res.put("aword", aword);
        final LinkedList<AssuranceType> ats = new LinkedList<>();
        for (AssuranceType at : AssuranceType.values()) {
            try {
                Notary.may(assurer, assuree, at);
                ats.add(at);
            } catch (GigiApiException e) {
            }
        }
        res.put("ats", new IterableDataset() {

            Iterator<AssuranceType> t = ats.iterator();

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if ( !t.hasNext()) {
                    return false;
                }
                AssuranceType t1 = t.next();
                vars.put("type", t1.getDescription());
                vars.put("id", t1.toString());
                vars.put("sel", t1 == type ? " selected" : "");
                return true;
            }
        });
        templ.output(out, l, res);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        location = req.getParameter("location");
        date = req.getParameter("date");
        if (date == null || location == null) {
            outputError(out, req, "You need to enter location and date!");
        }

        if ( !"1".equals(req.getParameter("certify")) || !"1".equals(req.getParameter("rules")) || !"1".equals(req.getParameter("tos_agree")) || !"1".equals(req.getParameter("assertion"))) {
            outputError(out, req, "You failed to check all boxes to validate" + " your adherence to the rules and policies of SomeCA");
        }
        if ("1".equals(req.getParameter("passwordReset"))) {
            aword = req.getParameter("passwordResetValue");
            if ("".equals(aword)) {
                aword = null;
            }
        } else {
            aword = null;
        }
        String val = req.getParameter("assuranceType");
        if (val != null) {
            try {
                type = AssuranceType.valueOf(val);
            } catch (IllegalArgumentException e) {
                outputError(out, req, "Assurance Type wrong.");
            }
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
            Notary.assure(assurer, assuree, assureeName, dob, pointsI, location, req.getParameter("date"), type);
            if (aword != null && !aword.equals("")) {
                String systemToken = RandomToken.generateToken(32);
                int id = assuree.generatePasswordResetTicket(Page.getUser(req), systemToken, aword);
                try {
                    Language l = Language.getInstance(assuree.getPreferredLocale());
                    StringBuffer body = new StringBuffer();
                    body.append(l.getTranslation("Hi,") + "\n\n");
                    body.append(l.getTranslation("A password reset was triggered. If you did a password reset by assurance, please enter your secret password using this form: \nhttps://"));
                    body.append(ServerConstants.getWwwHostNamePortSecure() + PasswordResetPage.PATH);
                    body.append("?id=");
                    body.append(id);
                    body.append("&token=");
                    body.append(URLEncoder.encode(systemToken, "UTF-8"));
                    body.append("\n");
                    body.append("\n");
                    body.append(l.getTranslation("Best regards"));
                    body.append("\n");
                    body.append(l.getTranslation("SomeCA.org Support!"));
                    Sendmail.getInstance().sendmail(assuree.getEmail(), "[SomeCA.org] " + l.getTranslation("Password reset by assurance"), body.toString(), "support@cacert.org", null, null, null, null, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
        }

        return false;
    }

    public User getAssuree() {
        return assuree;
    }

}
