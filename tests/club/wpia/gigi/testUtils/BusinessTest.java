package club.wpia.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.email.EmailProvider;
import club.wpia.gigi.ping.PingerDaemon;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.DayDate;

public abstract class BusinessTest extends ConfiguredTest {

    public static class InVMEmail extends EmailProvider implements MailReceiver {

        private static InVMEmail instance;

        LinkedBlockingQueue<TestMail> mails = new LinkedBlockingQueue<>();

        public InVMEmail(Properties p) {
            instance = this;
        }

        @Override
        public void sendMail(String to, String subject, String message, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
            TestMail tm = new TestEmailReceiver.TestMail(to, subject, message, replyto) {

                @Override
                public void verify() throws IOException {
                    Pattern p = Pattern.compile("type=(email|domain)&id=([0-9]+)&hash=([a-zA-Z0-9]*)");
                    Matcher m = p.matcher(extractLink());
                    assertTrue(m.find());
                    String type = m.group(1);
                    try {
                        if (type.equals("domain")) {
                            Domain.getById(Integer.parseInt(m.group(2))).verify(m.group(3));
                        } else {
                            EmailAddress.getById(Integer.parseInt(m.group(2))).verify(m.group(3));
                        }
                    } catch (GigiApiException e) {
                        throw new Error(e);
                    }
                }
            };
            mails.add(tm);
        }

        public static InVMEmail getInstance() {
            return instance;
        }

        @Override
        public void clearMails() {
            mails.clear();
        }

        @Override
        public TestMail receive() {
            TestMail poll;
            try {
                poll = mails.poll(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
            if (poll == null) {
                throw new AssertionError("Mail receiving timed out");
            }
            return poll;
        }

        @Override
        public void setApproveRegex(Pattern compiled) {
            throw new Error("Currently unimplemented");
        }

        @Override
        public void setEmailCheckError(String string) {
            throw new Error("Currently unimplemented");
        }

        @Override
        public TestMail poll() {
            throw new Error("Currently unimplemented");
        }

    }

    @BeforeClass
    public static void purgeDBBeforeTest() throws SQLException, IOException {
        purgeOnlyDB();
    }

    @BeforeClass
    public static void initMail() {
        Properties p = new Properties();
        p.setProperty("emailProvider", InVMEmail.class.getName());
        EmailProvider.initSystem(p, null, null);
        try {
            new PingerDaemon(KeyStore.getInstance("JKS")).start();
        } catch (KeyStoreException e) {
            throw new Error(e);
        }
    }

    public static User createVerifiedUser() throws GigiApiException, IOException {
        Calendar c = Calendar.getInstance();
        c.set(1950, 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);

        User u = new User(createUniqueName() + "@email.com", TEST_PASSWORD, new DayDate(c.getTimeInMillis()), Locale.ENGLISH, null, //
                new NamePart(NamePartType.FIRST_NAME, "a"), new NamePart(NamePartType.FIRST_NAME, "m"), new NamePart(NamePartType.LAST_NAME, "c"));
        InVMEmail.getInstance().mails.poll().verify();
        return u;
    }

    public static int createVerifiedUser(String f, String l, String mail, String pw) throws GigiApiException {
        User u = createUser(f, l, mail, pw);
        try {
            InVMEmail.getInstance().mails.poll().verify();
        } catch (IOException e) {
            throw new Error(e);
        }
        return u.getId();
    }

    public static User createUser(String f, String l, String mail, String pw) throws GigiApiException {
        Calendar c = Calendar.getInstance();
        c.set(1950, 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);

        User u = new User(mail, pw, new DayDate(c.getTimeInMillis()), Locale.ENGLISH, null, //
                new NamePart(NamePartType.FIRST_NAME, f), new NamePart(NamePartType.LAST_NAME, l));
        return u;
    }

    public static int createVerificationUser(String f, String l, String mail, String pw) throws GigiApiException {
        int u = createVerifiedUser(f, l, mail, pw);
        makeAgent(u);
        return u;
    }

    @Override
    public MailReceiver getMailReceiver() {
        return InVMEmail.getInstance();
    }

    private User supporter;

    public User getSupporter() throws GigiApiException, IOException {
        if (supporter != null) {
            return supporter;
        }
        supporter = createVerifiedUser();
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `user_groups` SET `user`=?, `permission`=?::`userGroup`, `grantedby`=?")) {
            ps.setInt(1, supporter.getId());
            ps.setString(2, Group.SUPPORTER.getDBName());
            ps.setInt(3, supporter.getId());
            ps.execute();
        }
        supporter.refreshGroups();
        return supporter;
    }
}
