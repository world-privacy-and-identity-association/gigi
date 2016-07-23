package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.crypto.SPKAC;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Assurance.AssuranceType;
import org.cacert.gigi.dbObjects.CATS;
import org.cacert.gigi.dbObjects.CATS.CATSType;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingType;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.NamePart;
import org.cacert.gigi.dbObjects.NamePart.NamePartType;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.account.certs.CertificateRequest;
import org.cacert.gigi.ping.DomainPinger;
import org.cacert.gigi.ping.PingerDaemon;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;

import sun.security.x509.X509Key;

public class Manager extends Page {

    public static final String PATH = "/manager";

    private static HashMap<DomainPingType, DomainPinger> dps;

    private Manager() {
        super("Test Manager");

        try {
            Field gigiInstance = Gigi.class.getDeclaredField("instance");
            gigiInstance.setAccessible(true);
            Gigi g = (Gigi) gigiInstance.get(null);

            Field gigiPinger = Gigi.class.getDeclaredField("pinger");
            gigiPinger.setAccessible(true);
            PingerDaemon pd = (PingerDaemon) gigiPinger.get(g);

            Field f = PingerDaemon.class.getDeclaredField("pingers");
            f.setAccessible(true);
            dps = (HashMap<DomainPingType, DomainPinger>) f.get(pd);
            HashMap<DomainPingType, DomainPinger> pingers = new HashMap<>();
            for (DomainPingType dpt : DomainPingType.values()) {
                pingers.put(dpt, new PingerFetcher(dpt));
            }
            f.set(pd, pingers);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public User getAssurer(int i) {
        if (assurers[i] != null) {
            return assurers[i];
        }
        try {
            User u = createAssurer(i);
            assurers[i] = u;

        } catch (ReflectiveOperationException | GigiApiException e) {
            e.printStackTrace();
        }
        return assurers[i];
    }

    private User createAssurer(int i) throws GigiApiException, IllegalAccessException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?")) {
            String mail = "test-assurer" + i + "@example.com";
            User u = User.getByEmail(mail);
            if (u == null) {
                System.out.println("Creating assurer");
                createUser(mail);
                u = User.getByEmail(mail);
                passCATS(u, CATSType.ASSURER_CHALLENGE);
                ps.setInt(1, u.getId());
                ps.setInt(2, u.getPreferredName().getId());
                ps.setInt(3, 100);
                ps.setString(4, "Manager init code");
                ps.setString(5, "1990-01-01");
                ps.execute();
            }
            return u;
        }
    }

    private void passCATS(User u, CATSType t) {
        CATS.enterResult(u, t, new Date(System.currentTimeMillis()), "en_EN", "1");
    }

    private static Manager instance;

    private static final Template t = new Template(Manager.class.getResource("ManagerMails.templ"));

    HashMap<String, LinkedList<String>> emails = new HashMap<>();

    private static TreeSet<String> pingExempt = new TreeSet<>();

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
        public synchronized void sendMail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
            HashMap<String, LinkedList<String>> mails = Manager.getInstance().emails;
            LinkedList<String> hismails = mails.get(to);
            if (hismails == null) {
                mails.put(to, hismails = new LinkedList<>());
            }
            hismails.addFirst(subject + "\n" + message);
        }

    }

    public class PingerFetcher extends DomainPinger {

        private DomainPingType dpt;

        public PingerFetcher(DomainPingType dpt) {
            this.dpt = dpt;
        }

        @Override
        public void ping(Domain domain, String configuration, CertificateOwner target, int confId) {
            System.out.println("Test: " + domain);
            if (pingExempt.contains(domain.getSuffix())) {
                enterPingResult(confId, DomainPinger.PING_SUCCEDED, "Succeeded by TestManager pass-by", null);
            } else {
                dps.get(dpt).ping(domain, configuration, target, confId);
            }
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
        Calendar gc = GregorianCalendar.getInstance();
        gc.setTimeInMillis(0);
        gc.set(1990, 0, 1);
        User u = new User(email, "xvXV12°§", new DayDate(gc.getTime().getTime()), Locale.ENGLISH, //
                new NamePart(NamePartType.FIRST_NAME, "Först"), new NamePart(NamePartType.FIRST_NAME, "Müddle"),//
                new NamePart(NamePartType.LAST_NAME, "Läst"), new NamePart(NamePartType.SUFFIX, "Süffix"));
        EmailAddress ea = u.getEmails()[0];
        verify(email, ea);
    }

    private void verify(String email, EmailAddress ea) throws GigiApiException {
        LinkedList<String> i = emails.get(email);
        while (i.size() > 0 && !ea.isVerified()) {
            String lst = i.getLast();
            Pattern p = Pattern.compile("hash=([a-zA-Z0-9]+)");
            Matcher m = p.matcher(lst);
            if (m.find()) {
                ea.verify(m.group(1));
            }
            i.removeLast();
        }
        // ea.verify(hash);
    }

    User[] assurers = new User[25];

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("create") != null) {
            batchCreateUsers(req.getParameter("prefix"), req.getParameter("suffix"), Integer.parseInt(req.getParameter("amount")), resp.getWriter());
            resp.getWriter().println("User batch created.");
        } else if (req.getParameter("addpriv") != null || req.getParameter("delpriv") != null) {
            User u = User.getByEmail(req.getParameter("email"));
            if (u == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            if (req.getParameter("addpriv") != null) {
                u.grantGroup(u, Group.getByString(req.getParameter("priv")));
                resp.getWriter().println("Privilege granted");
            } else {
                u.revokeGroup(u, Group.getByString(req.getParameter("priv")));
                resp.getWriter().println("Privilege revoked");
            }
        } else if (req.getParameter("fetch") != null) {
            String mail = req.getParameter("femail");
            fetchMails(req, resp, mail);
        } else if (req.getParameter("cats") != null) {
            String mail = req.getParameter("catsEmail");
            String testId = req.getParameter("catsType");
            User byEmail = User.getByEmail(mail);
            if (byEmail == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            if (testId == null) {
                resp.getWriter().println("No test given.");
                return;
            }
            CATSType test = CATSType.values()[Integer.parseInt(testId)];
            passCATS(byEmail, test);
            resp.getWriter().println("Test '" + test.getDisplayName() + "' was added to user account.");
        } else if (req.getParameter("assure") != null) {
            String mail = req.getParameter("assureEmail");
            String verificationPoints = req.getParameter("verificationPoints");
            User byEmail = User.getByEmail(mail);

            if (byEmail == null) {
                resp.getWriter().println("User not found.");
                return;
            }

            int vp = 0;
            int agentNumber = 0;

            try {
                try {
                    vp = Integer.parseInt(verificationPoints);
                } catch (NumberFormatException e) {
                    throw new GigiApiException("No valid Verification Points entered.");
                }

                if (vp > 100) { // only allow max 100 Verification points
                    vp = 100;
                }

                while (vp > 0) {
                    int currentVP = 10;
                    if (vp < 10) {
                        currentVP = vp;
                    }
                    Notary.assure(getAssurer(agentNumber), byEmail, byEmail.getPreferredName(), byEmail.getDoB(), currentVP, "Testmanager Assure up code", "2014-11-06", AssuranceType.FACE_TO_FACE);
                    agentNumber += 1;
                    vp -= currentVP;
                }

            } catch (GigiApiException e) {
                throw new Error(e);
            }

            resp.getWriter().println("User has been assured " + agentNumber + " times.");

        } else if (req.getParameter("letassure") != null) {
            String mail = req.getParameter("letassureEmail");
            User byEmail = User.getByEmail(mail);
            try {
                for (int i = 0; i < 25; i++) {
                    User a = getAssurer(i);
                    Notary.assure(byEmail, a, a.getNames()[0], a.getDoB(), 10, "Testmanager exp up code", "2014-11-06", AssuranceType.FACE_TO_FACE);
                }
            } catch (GigiApiException e) {
                throw new Error(e);
            }
        } else if (req.getParameter("addEmail") != null) {
            User u = User.getByEmail(req.getParameter("addEmailEmail"));
            try {
                EmailAddress ea = new EmailAddress(u, req.getParameter("addEmailNew"), Locale.ENGLISH);
                verify(ea.getAddress(), ea);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                resp.getWriter().println("An internal error occured.");
            } catch (GigiApiException e) {
                e.format(resp.getWriter(), Language.getInstance(Locale.ENGLISH));
            }
        } else if (req.getParameter("addCert") != null) {
            User u = User.getByEmail(req.getParameter("addCertEmail"));
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(4096);
                KeyPair kp = kpg.generateKeyPair();
                SPKAC s = new SPKAC((X509Key) kp.getPublic(), "challange");
                Signature sign = Signature.getInstance("SHA512withRSA");
                sign.initSign(kp.getPrivate());

                byte[] res = s.getEncoded(sign);

                CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u), Base64.getEncoder().encodeToString(res), "challange");
                cr.update(CertificateRequest.DEFAULT_CN, Digest.SHA512.toString(), "client", null, "", "email:" + u.getEmail());
                Certificate draft = cr.draft();
                draft.issue(null, "2y", u).waitFor(10000);
                if (draft.getStatus() == CertificateStatus.ISSUED) {
                    resp.getWriter().println("added certificate");
                } else {
                    resp.getWriter().println("signer failed");
                }
            } catch (GeneralSecurityException e1) {
                e1.printStackTrace();
                resp.getWriter().println("error");
            } catch (GigiApiException e) {
                e.format(resp.getWriter(), Language.getInstance(Locale.ENGLISH));
            } catch (InterruptedException e) {
                e.printStackTrace();
                resp.getWriter().println("interrupted");
            }

        } else if (req.getParameter("addExDom") != null) {
            String dom = req.getParameter("exemtDom");
            pingExempt.add(dom);
            resp.getWriter().println("Updated domains exempt from pings. Current set: <br/>");
            resp.getWriter().println(pingExempt);
        } else if (req.getParameter("delExDom") != null) {
            String dom = req.getParameter("exemtDom");
            pingExempt.remove(dom);
            resp.getWriter().println("Updated domains exempt from pings. Current set: <br/>");
            resp.getWriter().println(pingExempt);
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
                    if ( !s.hasNext()) {
                        return false;
                    }
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

    private static final Template form = new Template(Manager.class.getResource("Manager.templ"));

    @Override
    public boolean needsLogin() {
        return false;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() > 1 && pi.startsWith("/fetch-")) {
            String mail = pi.substring(pi.indexOf('-', 2) + 1);
            fetchMails(req, resp, mail);
            return;
        }
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("cats_types", new IterableDataset() {

            CATSType[] type = CATSType.values();

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i >= type.length) {
                    return false;
                }
                CATSType t = type[i++];
                vars.put("id", i - 1);
                vars.put("name", t.getDisplayName());
                return true;
            }
        });
        form.output(resp.getWriter(), getLanguage(req), vars);
    }
}
