package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.CalendarUtil;
import org.cacert.gigi.util.HTMLEncoder;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.PasswordStrengthChecker;

public class Signup extends Form {

    Name buildupName = new Name("", "", "", "");

    String email = "";

    private Template t;

    boolean general = true, country = true, regional = true, radius = true;

    public Signup(HttpServletRequest hsr) {
        super(hsr);
        t = new Template(Signup.class.getResource("Signup.templ"));
    }

    DateSelector myDoB = new DateSelector("day", "month", "year");

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> outerVars) {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("fname", HTMLEncoder.encodeHTML(buildupName.getFname()));
        vars.put("mname", HTMLEncoder.encodeHTML(buildupName.getMname()));
        vars.put("lname", HTMLEncoder.encodeHTML(buildupName.getLname()));
        vars.put("suffix", HTMLEncoder.encodeHTML(buildupName.getSuffix()));
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

    private void update(HttpServletRequest r) {
        String fname = buildupName.getFname();
        String lname = buildupName.getLname();
        String mname = buildupName.getMname();
        String suffix = buildupName.getSuffix();
        if (r.getParameter("fname") != null) {
            fname = r.getParameter("fname");
        }
        if (r.getParameter("lname") != null) {
            lname = r.getParameter("lname");
        }
        if (r.getParameter("mname") != null) {
            mname = r.getParameter("mname");
        }
        if (r.getParameter("suffix") != null) {
            suffix = r.getParameter("suffix");
        }
        if (r.getParameter("email") != null) {
            email = r.getParameter("email");
        }
        buildupName = new Name(fname, lname, mname, suffix);
        general = "1".equals(r.getParameter("general"));
        country = "1".equals(r.getParameter("country"));
        regional = "1".equals(r.getParameter("regional"));
        radius = "1".equals(r.getParameter("radius"));
        try {
            myDoB.update(r);
        } catch (GigiApiException e) {
        }
    }

    @Override
    public synchronized boolean submit(PrintWriter out, HttpServletRequest req) {
        update(req);
        if (buildupName.getLname().trim().equals("")) {
            outputError(out, req, "Last name were blank.");
        }
        if ( !myDoB.isValid()) {
            outputError(out, req, "Invalid date of birth");
        }

        if ( !CalendarUtil.isOfAge(myDoB.getDate(), User.MINIMUM_AGE)) {
            outputError(out, req, "Entered dated of birth is below the restricted age requirements.");
        }

        if ( !"1".equals(req.getParameter("tos_agree"))) {
            outputError(out, req, "Acceptance of the ToS is required to continue.");
        }
        if (email.equals("")) {
            outputError(out, req, "Email Address was blank");
        }
        String pw1 = req.getParameter("pword1");
        String pw2 = req.getParameter("pword2");
        if (pw1 == null || pw1.equals("")) {
            outputError(out, req, "Pass Phrases were blank");
        } else if ( !pw1.equals(pw2)) {
            outputError(out, req, "Pass Phrases don't match");
        }
        int pwpoints = PasswordStrengthChecker.checkpw(pw1, buildupName, email);
        if (pwpoints < 3) {
            outputError(out, req, "The Pass Phrase you submitted failed to contain enough" + " differing characters and/or contained words from" + " your name and/or email address.");
        }
        if (isFailed(out)) {
            return false;
        }
        try (GigiPreparedStatement q1 = new GigiPreparedStatement("SELECT * FROM `emails` WHERE `email`=? AND `deleted` IS NULL"); GigiPreparedStatement q2 = new GigiPreparedStatement("SELECT * FROM `certOwners` INNER JOIN `users` ON `users`.`id`=`certOwners`.`id` WHERE `email`=? AND `deleted` IS NULL")) {
            q1.setString(1, email);
            q2.setString(1, email);
            GigiResultSet r1 = q1.executeQuery();
            GigiResultSet r2 = q2.executeQuery();
            if (r1.next() || r2.next()) {
                outputError(out, req, "This email address is currently valid in the system.");
            }
        }
        try (GigiPreparedStatement q3 = new GigiPreparedStatement("SELECT `domain` FROM `baddomains` WHERE `domain`=RIGHT(?, LENGTH(`domain`))")) {
            q3.setString(1, email);

            GigiResultSet r3 = q3.executeQuery();
            if (r3.next()) {
                String domain = r3.getString(1);
                outputError(out, req, "We don't allow signups from people using email addresses from %s", domain);
            }
        }
        String mailResult = EmailProvider.FAIL;
        try {
            mailResult = HTMLEncoder.encodeHTML(EmailProvider.getInstance().checkEmailServer(0, email));
        } catch (IOException e) {
        }
        if ( !mailResult.equals(EmailProvider.OK)) {
            if (mailResult.startsWith("4")) {
                outputError(out, req, "The mail server responsible for your domain indicated" + " a temporary failure. This may be due to anti-SPAM measures, such" + " as greylisting. Please try again in a few minutes.");
            } else {
                outputError(out, req, "Email Address given was invalid, or a test connection" + " couldn't be made to your server, or the server" + " rejected the email address as invalid");
            }
            if (mailResult.equals(EmailProvider.FAIL)) {
                outputError(out, req, "Failed to make a connection to the mail server");
            } else {
                outputErrorPlain(out, mailResult);
            }
        }

        if (isFailed(out)) {
            return false;
        }
        if (RegisterPage.RATE_LIMIT.isLimitExceeded(req.getRemoteAddr())) {
            outputError(out, req, "Rate Limit Exceeded");
            return false;
        }
        try {
            run(req, pw1);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
            return false;
        }
        return true;
    }

    private void run(HttpServletRequest req, String password) throws SQLException, GigiApiException {
        User u = new User(email, password, buildupName, myDoB.getDate(), Page.getLanguage(req).getLocale());

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
