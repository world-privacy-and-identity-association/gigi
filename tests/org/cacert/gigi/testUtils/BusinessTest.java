package org.cacert.gigi.testUtils;

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

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.ping.PingerDaemon;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
import org.cacert.gigi.util.DayDate;
import org.junit.BeforeClass;

public abstract class BusinessTest extends ConfiguredTest {

    public static class InVMEmail extends EmailProvider implements MailReceiver {

        private static InVMEmail instance;

        LinkedBlockingQueue<TestMail> mails = new LinkedBlockingQueue<>();

        public InVMEmail(Properties p) {
            instance = this;
        }

        @Override
        public void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
            TestMail tm = new TestEmailReceiver.TestMail(to, subject, message, fromname, replyto) {

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
            try {
                return mails.poll(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
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

        User u = new User(createUniqueName() + "@email.com", TEST_PASSWORD, new Name("a", "m", "c", ""), new DayDate(c.getTimeInMillis()), Locale.ENGLISH);
        InVMEmail.getInstance().mails.poll().verify();
        return u;
    }

    public static int createVerifiedUser(String f, String l, String mail, String pw) throws GigiApiException {
        Calendar c = Calendar.getInstance();
        c.set(1950, 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);

        User u = new User(mail, pw, new Name(f, l, "", ""), new DayDate(c.getTimeInMillis()), Locale.ENGLISH);
        try {
            InVMEmail.getInstance().mails.poll().verify();
        } catch (IOException e) {
            throw new Error(e);
        }
        return u.getId();
    }

    public static int createAssuranceUser(String f, String l, String mail, String pw) throws GigiApiException {
        int u = createVerifiedUser(f, l, mail, pw);
        makeAssurer(u);
        return u;
    }

    @Override
    public MailReceiver getMailReciever() {
        return InVMEmail.getInstance();
    }
}
