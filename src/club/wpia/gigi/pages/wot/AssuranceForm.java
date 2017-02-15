package club.wpia.gigi.pages.wot;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Assurance.AssuranceType;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.ArrayIterable;
import club.wpia.gigi.output.CountrySelector;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.SprintfCommand;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.pages.PasswordResetPage;
import club.wpia.gigi.util.DayDate;
import club.wpia.gigi.util.Notary;

public class AssuranceForm extends Form {

    public static class ConcatOutputable implements Outputable {

        private Outputable[] outputables;

        public ConcatOutputable(Outputable... outputables) {
            this.outputables = outputables;
        }

        @Override
        public void output(PrintWriter out, Language l, Map<String, Object> vars) {
            for (int i = 0; i < outputables.length; i++) {
                if (i != 0) {
                    out.println();
                }
                outputables[i].output(out, l, vars);
            }
        }
    }

    private User assuree;

    private Name[] assureeNames;

    private boolean[] selected;

    private DayDate dob;

    private String location = "";

    private String date = "";

    private String aword;

    private User assurer;

    private AssuranceType type = AssuranceType.FACE_TO_FACE;

    private static final Template templ = new Template(AssuranceForm.class.getResource("AssuranceForm.templ"));

    private CountrySelector cs;

    public AssuranceForm(HttpServletRequest hsr, User assuree) throws GigiApiException {
        super(hsr);
        assurer = Page.getUser(hsr);
        this.assuree = assuree;

        if (assurer.getId() == assuree.getId()) {
            throw new GigiApiException("You cannot verify yourself.");
        }
        if ( !assurer.canAssure()) {
            throw new GigiApiException("You are not a RA-Agent.");
        }

        Name[] initialNames = this.assuree.getNonDeprecatedNames();
        LinkedList<Name> names = new LinkedList<>();
        for (Name name : initialNames) {
            if (Notary.checkAssuranceIsPossible(assurer, name)) {
                names.add(name);
            }
        }
        if (names.size() == 0) {
            throw new GigiApiException(SprintfCommand.createSimple("You have already verified all names of this applicant within the last {0} days.", Notary.LIMIT_DAYS_VERIFICATION));
        }
        assureeNames = names.toArray(new Name[names.size()]);
        dob = this.assuree.getDoB();
        selected = new boolean[assureeNames.length];
        cs = new CountrySelector("countryCode", false);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat sdf2 = new SimpleDateFormat("dd. MMM yyyy");

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        res.putAll(vars);
        res.put("names", new ArrayIterable<Name>(assureeNames) {

            @Override
            public void apply(Name t, Language l, Map<String, Object> vars) {
                vars.put("nameExplicit", t);
                vars.put("nameId", t.getId());
                vars.put("checked", selected[i] ? " checked" : "");
            }

        });
        res.put("name", assuree.getPreferredName().toString());
        res.put("maxpoints", assurer.getMaxAssurePoints());
        res.put("dob", sdf.format(assuree.getDoB().toDate()));
        res.put("dobFmt2", sdf2.format(assuree.getDoB().toDate()));
        res.put("location", location);
        res.put("date", date);
        res.put("aword", aword);
        res.put("countryCode", cs);

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
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        location = req.getParameter("location");
        date = req.getParameter("date");
        cs.update(req);
        GigiApiException gae = new GigiApiException();
        if (date == null || location == null) {
            gae.mergeInto(new GigiApiException("You need to enter location and date!"));
        }

        if ( !"1".equals(req.getParameter("certify")) || !"1".equals(req.getParameter("rules")) || !"1".equals(req.getParameter("assertion"))) {
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
                gae.mergeInto(new GigiApiException("Verification Type wrong."));
            }
        }

        int pointsI = 0;
        String points = req.getParameter("points");
        if (points == null || "".equals(points)) {
            gae.mergeInto(new GigiApiException("For a verification, you need to enter points."));
        } else {
            try {
                pointsI = Integer.parseInt(points);
            } catch (NumberFormatException e) {
                gae.mergeInto(new GigiApiException("The points entered were not a number."));
            }
        }
        String[] parameterValues = req.getParameterValues("assuredName");
        HashSet<String> data = new HashSet<>(Arrays.asList(parameterValues == null ? new String[0] : parameterValues));
        for (int i = 0; i < assureeNames.length; i++) {
            selected[i] = data.contains(Integer.toString(assureeNames[i].getId()));
        }

        if ( !gae.isEmpty()) {
            throw gae;
        }

        LinkedList<Name> toAssure = new LinkedList<Name>();
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                toAssure.add(assureeNames[i]);
            }
        }
        if (toAssure.size() == 0) {
            throw new GigiApiException("You must confirm at least one name to verify an account.");
        }

        Notary.assureAll(assurer, assuree, dob, pointsI, location, req.getParameter("date"), type, toAssure.toArray(new Name[toAssure.size()]), cs.getCountry());
        Outputable result = new TranslateCommand("Verification complete.");
        if (isWithPasswordReset()) {
            Language langApplicant = Language.getInstance(assuree.getPreferredLocale());
            String method = langApplicant.getTranslation("A password reset was triggered. If you did a password reset by verification, please enter your secret password using this form:");
            String subject = langApplicant.getTranslation("Password reset by verification");
            PasswordResetPage.initPasswordResetProcess(assuree, req, aword, langApplicant, method, subject);
            result = new ConcatOutputable(result, new TranslateCommand("Password reset successful."));
        }
        return new SuccessMessageResult(result);
    }

    public boolean isWithPasswordReset() {
        return aword != null && !aword.equals("");
    }

    public User getAssuree() {
        return assuree;
    }

}
