package club.wpia.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.crypto.SPKAC;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.CATS;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Contract;
import club.wpia.gigi.dbObjects.Contract.ContractType;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingExecution;
import club.wpia.gigi.dbObjects.DomainPingType;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.Verification.VerificationType;
import club.wpia.gigi.email.DelegateMailProvider;
import club.wpia.gigi.email.EmailProvider;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.ping.DomainPinger;
import club.wpia.gigi.ping.PingerDaemon;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.DayDate;
import club.wpia.gigi.util.DomainAssessment;
import club.wpia.gigi.util.HTMLEncoder;
import club.wpia.gigi.util.Notary;
import club.wpia.gigi.util.TimeConditions;
import sun.security.x509.X509Key;

public class Manager extends Page {

    public static String validVerificationDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -Notary.LIMIT_MAX_MONTHS_VERIFICATION + 1);
        return sdf.format(new Date(c.getTimeInMillis()));
    }

    public static Country getRandomCountry() {
        List<Country> cc = Country.getCountries();
        int rnd = new Random().nextInt(cc.size());
        return cc.get(rnd);
    }

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

    public User getSupporter() {
        if (supporter != null) {
            return supporter;
        }
        try {
            User u = createAgent( -1);
            if ( !u.isInGroup(Group.SUPPORTER)) {
                try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `user_groups` SET `user`=?, `permission`=?::`userGroup`, `grantedby`=?")) {
                    ps.setInt(1, u.getId());
                    ps.setString(2, Group.SUPPORTER.getDBName());
                    ps.setInt(3, u.getId());
                    ps.execute();
                }
                u.refreshGroups();
            }
            supporter = u;
        } catch (ReflectiveOperationException | GigiApiException e) {
            e.printStackTrace();
        }
        return supporter;
    }

    public User getAgent(int i) {
        if (agents[i] != null) {
            return agents[i];
        }
        try {
            User u = createAgent(i);
            agents[i] = u;

        } catch (ReflectiveOperationException | GigiApiException e) {
            e.printStackTrace();
        }
        return agents[i];
    }

    private User createAgent(int i) throws GigiApiException, IllegalAccessException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `country`=?")) {
            String mail = "test-agent" + i + "@example.com";
            User u = User.getByEmail(mail);
            if (u == null) {
                System.out.println("Creating RA-Agent");
                createUser(mail);
                u = User.getByEmail(mail);
                passCATS(u, CATSType.AGENT_CHALLENGE);
                ps.setInt(1, u.getId());
                ps.setInt(2, u.getPreferredName().getId());
                ps.setInt(3, 100);
                ps.setString(4, "Manager init code");
                ps.setString(5, "1990-01-01");
                ps.setString(6, getRandomCountry().getCode());
                ps.execute();
            }
            new Contract(u, ContractType.RA_AGENT_CONTRACT);
            return u;
        }
    }

    private void passCATS(User u, CATSType t) {
        CATS.enterResult(u, t, new Date(System.currentTimeMillis()), "en_EN", "1");
    }

    private void expireCATS(User u, CATSType t) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id` FROM `cats_passed` WHERE `user_id`=? AND `variant_id`=? AND `pass_date`>?")) {
            ps.setInt(1, u.getId());
            ps.setInt(2, t.getId());
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 366));
            ps.execute();
            GigiResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GigiPreparedStatement ps1 = new GigiPreparedStatement("UPDATE `cats_passed` SET `pass_date`=? WHERE `id`=?");
                ps1.setTimestamp(1, new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 367));
                ps1.setInt(2, rs.getInt(1));
                ps1.execute();
                ps1.close();
            }
        }

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

    public static class MailFetcher extends DelegateMailProvider {

        Pattern[] toForward;

        public MailFetcher(Properties props) {
            super(props, props.getProperty("emailProvider.manager.target"));
            String str = props.getProperty("emailProvider.manager.filter");
            if (str == null) {
                toForward = new Pattern[0];
            } else {
                String[] parts = str.split(" ");
                toForward = new Pattern[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    toForward[i] = Pattern.compile(parts[i]);
                }
            }
        }

        @Override
        public String checkEmailServer(int forUid, String address) throws IOException {
            return OK;
        }

        @Override
        public synchronized void sendMail(String to, String subject, String message, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
            HashMap<String, LinkedList<String>> mails = Manager.getInstance().emails;
            LinkedList<String> hismails = mails.get(to);
            if (hismails == null) {
                mails.put(to, hismails = new LinkedList<>());
            }
            hismails.addFirst(subject + "\n" + message);
            for (int i = 0; i < toForward.length; i++) {
                if (toForward[i].matcher(to).matches()) {
                    super.sendMail(to, subject, message, replyto, toname, fromname, errorsto, extra);
                    return;
                }
            }
        }

    }

    public class PingerFetcher extends DomainPinger {

        private DomainPingType dpt;

        public PingerFetcher(DomainPingType dpt) {
            this.dpt = dpt;
        }

        @Override
        public DomainPingExecution ping(Domain domain, String configuration, CertificateOwner target, DomainPingConfiguration conf) {
            System.err.println("TestManager: " + domain.getSuffix());
            if (pingExempt.contains(domain.getSuffix())) {
                return enterPingResult(conf, DomainPinger.PING_SUCCEDED, "Succeeded by TestManager pass-by", null);
            } else {
                DomainPinger pinger = dps.get(dpt);
                System.err.println("Forward to old pinger: " + pinger);
                return pinger.ping(domain, configuration, target, conf);
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

        Country country = getRandomCountry();

        User u = new User(email, "xvXV12°§", new DayDate(gc.getTime().getTime()), Locale.ENGLISH, country, //
                new NamePart(NamePartType.FIRST_NAME, "Först"), new NamePart(NamePartType.FIRST_NAME, "Müddle"), //
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

    User[] agents = new User[25];

    User supporter;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AuthorizationContext sessionAc = (AuthorizationContext) req.getSession().getAttribute(Gigi.AUTH_CONTEXT);
        if (req.getParameter("create") != null) {
            String prefix = req.getParameter("prefix");
            String domain = req.getParameter("suffix");
            try {
                if (null == prefix) {
                    throw new GigiApiException("No prefix given.");
                }
                if (null == domain) {
                    throw new GigiApiException("No domain given.");
                }

                DomainAssessment.checkCertifiableDomain(domain, false, true);

                if ( !EmailProvider.isValidMailAddress(prefix + "@" + domain)) {
                    throw new GigiApiException("Invalid email address template.");
                }

                batchCreateUsers(prefix, domain, Integer.parseInt(req.getParameter("amount")), resp.getWriter());
                resp.getWriter().println("User batch created.");
            } catch (GigiApiException e) {
                throw new Error(e);
            }
        } else if (req.getParameter("addpriv") != null || req.getParameter("delpriv") != null) {
            User u = User.getByEmail(req.getParameter("email"));
            if (u == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            try {
                if (req.getParameter("addpriv") != null) {
                    u.grantGroup(getSupporter(), Group.getByString(req.getParameter("priv")));
                    resp.getWriter().println("Privilege granted");
                } else {
                    u.revokeGroup(getSupporter(), Group.getByString(req.getParameter("priv")));
                    resp.getWriter().println("Privilege revoked");
                }
            } catch (GigiApiException e) {
                throw new Error(e);
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
        } else if (req.getParameter("catsexpire") != null) {
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
            expireCATS(byEmail, test);
            resp.getWriter().println("Test '" + test.getDisplayName() + "' is set expired for user account.");
        } else if (req.getParameter("verify") != null) {
            String mail = req.getParameter("verifyEmail");
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
                    Notary.verify(getAgent(agentNumber), byEmail, byEmail.getPreferredName(), byEmail.getDoB(), currentVP, "Testmanager Verify up code", validVerificationDateString(), VerificationType.FACE_TO_FACE, getRandomCountry());
                    agentNumber += 1;
                    vp -= currentVP;
                }

            } catch (GigiApiException e) {
                throw new Error(e);
            }

            resp.getWriter().println("User has been verified " + agentNumber + " times.");

        } else if (req.getParameter("letverify") != null) {
            String mail = req.getParameter("letverifyEmail");
            User byEmail = User.getByEmail(mail);
            if (byEmail == null || !byEmail.canVerify()) {
                resp.getWriter().println("User not found, or found user is not allowed to verify.");
            } else {
                try {
                    for (int i = 0; i < 25; i++) {
                        User a = getAgent(i);
                        Notary.verify(byEmail, a, a.getNames()[0], a.getDoB(), 10, "Testmanager exp up code", validVerificationDateString(), VerificationType.FACE_TO_FACE, getRandomCountry());
                    }
                    resp.getWriter().println("Successfully added experience points.");
                } catch (GigiApiException e) {
                    throw new Error(e);
                }
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
                e.format(resp.getWriter(), Language.getInstance(Locale.ENGLISH), getDefaultVars(req));
            }
        } else if (req.getParameter("addCert") != null) {
            User u = User.getByEmail(req.getParameter("addCertEmail"));
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(4096);
                KeyPair kp = kpg.generateKeyPair();
                SPKAC s = new SPKAC((X509Key) kp.getPublic(), "challenge");
                Signature sign = Signature.getInstance("SHA512withRSA");
                sign.initSign(kp.getPrivate());

                byte[] res = s.getEncoded(sign);

                CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u, sessionAc.isStronglyAuthenticated()), Base64.getEncoder().encodeToString(res), "challenge");
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
                e.format(resp.getWriter(), Language.getInstance(Locale.ENGLISH), getDefaultVars(req));
            }

        } else if (req.getParameter("addExDom") != null) {
            String dom = req.getParameter("exemptDom");
            pingExempt.add(dom);
            resp.getWriter().println("Updated domains exempt from pings. Current set: <br/>");
            resp.getWriter().println(HTMLEncoder.encodeHTML(pingExempt.toString()));
        } else if (req.getParameter("delExDom") != null) {
            String dom = req.getParameter("exemptDom");
            pingExempt.remove(dom);
            resp.getWriter().println("Updated domains exempt from pings. Current set: <br/>");
            resp.getWriter().println(HTMLEncoder.encodeHTML(pingExempt.toString()));
        }
        resp.getWriter().println("<br/><a href='" + PATH + "'>Go back</a>");
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

        vars.put("testValidMonths", TimeConditions.getInstance().getTestMonths());
        vars.put("reverificationDays", TimeConditions.getInstance().getVerificationLimitDays());
        vars.put("verificationFreshMonths", TimeConditions.getInstance().getVerificationMonths());
        vars.put("verificationMaxAgeMonths", TimeConditions.getInstance().getVerificationMaxAgeMonths());
        vars.put("emailPingMonths", TimeConditions.getInstance().getEmailPingMonths());

        form.output(resp.getWriter(), getLanguage(req), vars);
    }
}
