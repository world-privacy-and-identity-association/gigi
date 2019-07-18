package club.wpia.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.email.EmailProvider;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.CountrySelector;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.output.NameInput;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.PlainOutputable;
import club.wpia.gigi.output.template.SprintfCommand;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.CalendarUtil;
import club.wpia.gigi.util.HTMLEncoder;
import club.wpia.gigi.util.Notary;
import club.wpia.gigi.util.RateLimit.RateLimitException;

public class Signup extends Form {

    private NameInput ni;

    private String email = "";

    private static final Template t = new Template(Signup.class.getResource("Signup.templ"));

    private CountrySelector cs;

    public Signup(HttpServletRequest hsr) {
        super(hsr);
        ni = new NameInput();
        cs = new CountrySelector("residenceCountry", true);
    }

    private DateSelector myDoB = new DateSelector("day", "month", "year");

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> outerVars) {
        HashMap<String, Object> vars = new HashMap<String, Object>(outerVars);
        vars.put("name", ni);
        vars.put("dob", myDoB);
        vars.put("email", HTMLEncoder.encodeHTML(email));
        vars.put("helpOnNames", new SprintfCommand("Help on Names {0}in the knowledge base{1}", Arrays.asList("!(/kb/names", "!'</a>")));
        vars.put("csrf", getCSRFToken());
        vars.put("dobmin", User.MINIMUM_AGE + "");
        vars.put("countryCode", cs);
        t.output(out, l, vars);
    }

    private void update(HttpServletRequest r) throws GigiApiException {
        if (r.getParameter("email") != null) {
            email = r.getParameter("email");
        }
        GigiApiException problems = new GigiApiException();
        try {
            ni.update(r);
        } catch (GigiApiException e) {
            problems.mergeInto(e);
        }
        try {
            myDoB.update(r);
        } catch (GigiApiException e) {
            problems.mergeInto(e);
        }

        cs.update(r);

        if ( !problems.isEmpty()) {
            throw problems;
        }

    }

    @Override
    public synchronized SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (RegisterPage.RATE_LIMIT.isLimitExceeded(req.getRemoteAddr())) {
            throw new RateLimitException();
        }

        GigiApiException ga = new GigiApiException();
        try {
            update(req);
        } catch (GigiApiException e) {
            ga.mergeInto(e);
        }
        try {
            ni.getNameParts();
        } catch (GigiApiException e) {
            ga.mergeInto(e);
        }

        if ( !myDoB.isValid()) {
            ga.mergeInto(new GigiApiException("Invalid date of birth"));
        }

        if ( !CalendarUtil.isOfAge(myDoB.getDate(), User.MINIMUM_AGE)) {
            ga.mergeInto(new GigiApiException("Entered date of birth is below the restricted age requirements."));
        }

        if (CalendarUtil.isYearsInFuture(myDoB.getDate().end(), User.MAXIMUM_PLAUSIBLE_AGE)) {
            ga.mergeInto(new GigiApiException("Entered date of birth exceeds the maximum age set in our policies. Please check your DoB is correct and contact support if the issue persists."));
        }

        if (email.equals("")) {
            ga.mergeInto(new GigiApiException("Email Address was blank"));
        }
        String pw1 = req.getParameter("pword1");
        String pw2 = req.getParameter("pword2");
        if (pw1 == null || pw1.equals("")) {
            ga.mergeInto(new GigiApiException("Passwords were blank"));
        } else if ( !pw1.equals(pw2)) {
            ga.mergeInto(new GigiApiException("Passwords don't match"));
        }

        if ( !"1".equals(req.getParameter("tos_agree"))) {
            ga.mergeInto(new GigiApiException("Acceptance of the ToS is required to continue."));
        }

        if ( !"1".equals(req.getParameter("dp_agree"))) {
            ga.mergeInto(new GigiApiException("Acceptance of the Data Protection Policy is required to continue."));
        }

        if ( !ga.isEmpty()) {
            throw ga;
        }
        GigiApiException gaPassword = Gigi.getPasswordChecker().checkPassword(pw1, ni.getNamePartsPlain(), email);
        if (gaPassword != null) {
            throw gaPassword;
        }
        GigiApiException ga2 = new GigiApiException();
        try (GigiPreparedStatement q1 = new GigiPreparedStatement("SELECT * FROM `emails` WHERE `email`=? AND `deleted` IS NULL"); GigiPreparedStatement q2 = new GigiPreparedStatement("SELECT * FROM `certOwners` INNER JOIN `users` ON `users`.`id`=`certOwners`.`id` WHERE `email`=? AND `deleted` IS NULL")) {
            q1.setString(1, email);
            q2.setString(1, email);
            GigiResultSet r1 = q1.executeQuery();
            GigiResultSet r2 = q2.executeQuery();
            if (r1.next() || r2.next()) {
                ga2.mergeInto(new GigiApiException("This email address is currently valid in the system."));
            }
        }
        try (GigiPreparedStatement q3 = new GigiPreparedStatement("SELECT `domain` FROM `baddomains` WHERE `domain`=RIGHT(?, LENGTH(`domain`))")) {
            q3.setString(1, email);

            GigiResultSet r3 = q3.executeQuery();
            if (r3.next()) {
                String domain = r3.getString(1);
                ga2.mergeInto(new GigiApiException(SprintfCommand.createSimple("We don't allow signups from people using email addresses from {0}.", domain)));
            }
        }
        String mailResult = EmailProvider.FAIL;
        try {
            mailResult = HTMLEncoder.encodeHTML(EmailProvider.getInstance().checkEmailServer(0, email));
        } catch (IOException e) {
        }
        if ( !mailResult.equals(EmailProvider.OK)) {
            if (mailResult.startsWith("4")) {
                ga2.mergeInto(new GigiApiException("The mail server responsible for your domain indicated" + " a temporary failure. This may be due to anti-SPAM measures, such" + " as greylisting. Please try again in a few minutes."));
            } else {
                ga2.mergeInto(new GigiApiException("Email Address given was invalid, or a test connection" + " couldn't be made to your server, or the server" + " rejected the email address as invalid"));
            }
            if (mailResult.equals(EmailProvider.FAIL)) {
                ga2.mergeInto(new GigiApiException("Failed to make a connection to the mail server"));
            } else {
                ga2.mergeInto(new GigiApiException(new PlainOutputable(mailResult)));
            }
        }

        if ( !ga2.isEmpty()) {
            throw ga2;
        }
        run(req, pw1);
        return new SuccessMessageResult(new TranslateCommand("Your information has been submitted" + " into our system. You will now be sent an email with a web link," + " you need to open that link in your web browser within 24 hours" + " or your information will be removed from our system!"));
    }

    private void run(HttpServletRequest req, String password) throws GigiApiException {
        User u = new User(email, password, myDoB.getDate(), Page.getLanguage(req).getLocale(), cs.getCountry(), ni.getNameParts());
        Notary.writeUserAgreement(u, "ToS", "account creation", "", true, 0);
        Notary.writeUserAgreement(u, "Data Protection Policy", "account creation", "", true, 0);
    }

}
