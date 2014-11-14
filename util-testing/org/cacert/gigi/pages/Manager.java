package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.util.Notary;

public class Manager extends Page {

    public static final String PATH = "/manager";

    Field f;

    private Manager() {
        super("Test Manager");
        try {
            f = EmailAddress.class.getDeclaredField("hash");
            f.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    public User[] getAssurers() {
        if (assurers != null) {
            return assurers;
        }
        assurers = new User[10];
        try {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?");
            for (int i = 0; i < assurers.length; i++) {
                String mail = "test-assurer" + i + "@example.com";
                User u = User.getByEmail(mail);
                if (u == null) {
                    createUser(mail);
                    u = User.getByEmail(mail);
                    passCATS(u);
                    ps.setInt(1, u.getId());
                    ps.setInt(2, u.getId());
                    ps.setInt(3, 100);
                    ps.setString(4, "Manager init code");
                    ps.setString(5, "1990-01-01");
                    ps.execute();
                }
                assurers[i] = u;

            }
        } catch (ReflectiveOperationException | GigiApiException e) {
            e.printStackTrace();
        }
        return assurers;
    }

    private void passCATS(User u) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO cats_passed SET user_id=?, variant_id=3");
        ps.setInt(1, u.getId());
        ps.execute();
    }

    private static Manager instance;

    Template t = new Template(Manager.class.getResource("ManagerMails.templ"));

    HashMap<String, LinkedList<String>> emails = new HashMap<>();

    public static Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public static class MailFetcher extends EmailProvider {

        public MailFetcher(Properties p) {}

        @Override
        public String checkEmailServer(int forUid, String address) throws IOException {
            return OK;
        }

        @Override
        public synchronized void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
            HashMap<String, LinkedList<String>> mails = Manager.getInstance().emails;
            LinkedList<String> hismails = mails.get(to);
            if (hismails == null) {
                mails.put(to, hismails = new LinkedList<>());
            }
            hismails.addFirst(subject + "\n" + message);
        }

    }

    public class ManagementForm extends Form {

        public ManagementForm(HttpServletRequest hsr) {
            super(hsr);
        }

        @Override
        public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
            return false;
        }

        @Override
        protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
            getDefaultTemplate().output(out, l, vars);
        }

    }

    public void batchCreateUsers(String mailPrefix, String domain, int amount, PrintWriter out) {

        try {
            if (amount > 100) {
                out.print("100 at most, please.");
                return;
            }
            for (int i = 0; i < amount; i++) {
                String email = mailPrefix + i + "@" + domain;
                createUser(email);
            }
        } catch (ReflectiveOperationException e) {
            out.println("failed");
            e.printStackTrace();
        } catch (GigiApiException e) {
            out.println("failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createUser(String email) throws GigiApiException, IllegalAccessException {
        User u = new User();
        u.setFname("Först");
        u.setMname("Müddle");
        u.setLname("Läst");
        u.setSuffix("Süffix");
        u.setEmail(email);
        Calendar gc = GregorianCalendar.getInstance();
        gc.set(1990, 0, 1);
        u.setDob(new Date(gc.getTime().getTime()));
        u.setPreferredLocale(Locale.ENGLISH);
        u.insert("xvXV12°§");
        EmailAddress ea = new EmailAddress(u, email);
        ea.insert(Language.getInstance(Locale.ENGLISH));
        String hash = (String) f.get(ea);

        ea.verify(hash);
    }

    User[] assurers;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("create") != null) {
            batchCreateUsers(req.getParameter("prefix"), req.getParameter("suffix"), Integer.parseInt(req.getParameter("amount")), resp.getWriter());
        } else if (req.getParameter("addpriv") != null || req.getParameter("delpriv") != null) {
            User u = User.getByEmail(req.getParameter("email"));
            if (u == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            if (req.getParameter("addpriv") != null) {
                u.grantGroup(u, Group.getByString(req.getParameter("priv")));
            } else {
                u.revokeGroup(u, Group.getByString(req.getParameter("priv")));
            }

        } else if (req.getParameter("fetch") != null) {
            String mail = req.getParameter("femail");
            fetchMails(req, resp, mail);
        } else if (req.getParameter("cats") != null) {
            String mail = req.getParameter("catsEmail");
            User byEmail = User.getByEmail(mail);
            if (byEmail == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            passCATS(byEmail);
        } else if (req.getParameter("assure") != null) {
            String mail = req.getParameter("assureEmail");
            User byEmail = User.getByEmail(mail);
            if (byEmail == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            try {
                for (int i = 0; i < getAssurers().length; i++) {
                    Notary.assure(getAssurers()[i], byEmail, byEmail.getName(), byEmail.getDob(), 10, "Testmanager Assure up code", "2014-11-06");
                }
            } catch (GigiApiException e) {
                throw new Error(e);
            }
        }
    }

    private void fetchMails(HttpServletRequest req, HttpServletResponse resp, String mail) throws IOException {
        final LinkedList<String> mails = emails.get(mail);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("mail", mail);
        if (mails != null) {
            vars.put("mails", new IterableDataset() {

                Iterator<String> s = mails.iterator();

                @Override
                public boolean next(Language l, Map<String, Object> vars) {
                    if ( !s.hasNext())
                        return false;
                    vars.put("body", s.next().replaceAll("(https?://\\S+)", "<a href=\"$1\">$1</a>"));
                    return true;
                }
            });
        }
        t.output(resp.getWriter(), getLanguage(req), vars);
        if (mails == null) {
            resp.getWriter().println("No mails");

        }
    }

    @Override
    public boolean needsLogin() {
        return false;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        getAssurers();
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() > 1 && pi.startsWith("/fetch-")) {
            String mail = pi.substring(pi.indexOf('-', 2) + 1);
            fetchMails(req, resp, mail);
            return;
        }

        new ManagementForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }
}
