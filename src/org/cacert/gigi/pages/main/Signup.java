package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.NameInput;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.PlainOutputable;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.CalendarUtil;
import org.cacert.gigi.util.HTMLEncoder;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.PasswordStrengthChecker;
import org.cacert.gigi.util.RateLimit.RateLimitException;

public class Signup extends Form {

    private NameInput ni;

    private String email = "";

    private static final Template t = new Template(Signup.class.getResource("Signup.templ"));

    private boolean general = true, country = true, regional = true, radius = true;

    public Signup(HttpServletRequest hsr) {
        super(hsr);
        ni = new NameInput();
    }

    private DateSelector myDoB = new DateSelector("day", "month", "year");

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> outerVars) {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("name", ni);
        vars.put("dob", myDoB);
        vars.put("email", HTMLEncoder.encodeHTML(email));
        vars.put("general", general ? " checked=\"checked\"" : "");
        vars.put("country", country ? " checked=\"checked\"" : "");
        vars.put("regional", regional ? " checked=\"checked\"" : "");
        vars.put("radius", radius ? " checked=\"checked\"" : "");
        vars.put("helpOnNames", String.format(l.getTranslation("Help on Names %sin the wiki%s"), "<a href=\"//wiki.cacert.org/FAQ/HowToEnterNamesInJoinForm\" target=\"_blank\">", "</a>"));
        vars.put("csrf", getCSRFToken());
        vars.put("dobmin", User.MINIMUM_AGE + "");
        t.output(out, l, vars);
    }

    private void update(HttpServletRequest r) throws GigiApiException {
        if (r.getParameter("email") != null) {
            email = r.getParameter("email");
        }
        general = "1".equals(r.getParameter("general"));
        country = "1".equals(r.getParameter("country"));
        regional = "1".equals(r.getParameter("regional"));
        radius = "1".equals(r.getParameter("radius"));
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
        if ( !problems.isEmpty()) {
            throw problems;
        }
    }

    @Override
    public synchronized boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
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

        if (CalendarUtil.isOfAge(myDoB.getDate(), User.MAXIMUM_PLAUSIBLE_AGE)) {
            ga.mergeInto(new GigiApiException("Entered date of birth exceeds the maximum age set in our policies. Please check your DoB is correct and contact support if the issue persists."));
        }

        if ( !"1".equals(req.getParameter("tos_agree"))) {
            ga.mergeInto(new GigiApiException("Acceptance of the ToS is required to continue."));
        }
        if (email.equals("")) {
            ga.mergeInto(new GigiApiException("Email Address was blank"));
        }
        String pw1 = req.getParameter("pword1");
        String pw2 = req.getParameter("pword2");
        if (pw1 == null || pw1.equals("")) {
            ga.mergeInto(new GigiApiException("Pass Phrases were blank"));
        } else if ( !pw1.equals(pw2)) {
            ga.mergeInto(new GigiApiException("Pass Phrases don't match"));
        }
        int pwpoints = PasswordStrengthChecker.checkpw(pw1, ni.getNamePartsPlain(), email);
        if (pwpoints < 3) {
            ga.mergeInto(new GigiApiException("The Pass Phrase you submitted failed to contain enough" + " differing characters and/or contained words from" + " your name and/or email address."));
        }
        if ( !ga.isEmpty()) {
            throw ga;
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
        return true;
    }

    private void run(HttpServletRequest req, String password) throws GigiApiException {
        User u = new User(email, password, myDoB.getDate(), Page.getLanguage(req).getLocale(), ni.getNameParts());

        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `alerts` SET `memid`=?," + " `general`=?, `country`=?, `regional`=?, `radius`=?")) {
            ps.setInt(1, u.getId());
            ps.setBoolean(2, general);
            ps.setBoolean(3, country);
            ps.setBoolean(4, regional);
            ps.setBoolean(5, radius);
            ps.execute();
        }
        Notary.writeUserAgreement(u, "ToS", "account creation", "", true, 0);

    }
}
