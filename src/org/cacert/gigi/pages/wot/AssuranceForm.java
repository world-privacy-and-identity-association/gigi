package org.cacert.gigi.pages.wot;

import java.io.PrintWriter;
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
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.PasswordResetPage;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;

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
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        location = req.getParameter("location");
        date = req.getParameter("date");
        GigiApiException gae = new GigiApiException();
        if (date == null || location == null) {
            gae.mergeInto(new GigiApiException("You need to enter location and date!"));
        }

        if ( !"1".equals(req.getParameter("certify")) || !"1".equals(req.getParameter("rules")) || !"1".equals(req.getParameter("tos_agree")) || !"1".equals(req.getParameter("assertion"))) {
            gae.mergeInto(new GigiApiException("You failed to check all boxes to validate" + " your adherence to the rules and policies of SomeCA"));
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
                gae.mergeInto(new GigiApiException("Assurance Type wrong."));
            }
        }

        int pointsI = 0;
        String points = req.getParameter("points");
        if (points == null || "".equals(points)) {
            gae.mergeInto(new GigiApiException("For an assurance, you need to enter points."));
        } else {
            try {
                pointsI = Integer.parseInt(points);
            } catch (NumberFormatException e) {
                gae.mergeInto(new GigiApiException("The points entered were not a number."));
            }
        }

        if ( !gae.isEmpty()) {
            throw gae;
        }
        Notary.assure(assurer, assuree, assureeName, dob, pointsI, location, req.getParameter("date"), type);
        if (aword != null && !aword.equals("")) {
            Language l = Language.getInstance(assuree.getPreferredLocale());
            String method = l.getTranslation("A password reset was triggered. If you did a password reset by assurance, please enter your secret password using this form:");
            String subject = l.getTranslation("Password reset by assurance");
            PasswordResetPage.initPasswordResetProcess(out, assuree, req, aword, l, method, subject);
        }
        return true;
    }

    public User getAssuree() {
        return assuree;
    }

}
