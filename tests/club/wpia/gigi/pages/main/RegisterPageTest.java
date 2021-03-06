package club.wpia.gigi.pages.main;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.testUtils.InitTruststore;
import club.wpia.gigi.testUtils.ManagedTest;

public class RegisterPageTest extends ManagedTest {

    static {
        InitTruststore.run();
        HttpURLConnection.setFollowRedirects(false);
        try {
            p = "&pword1=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&pword2=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public static final String p;

    @Before
    public void setUp() throws Exception {
        clearCaches(); // We do many registers in this test suite.
    }

    private static String createBase() {
        return createUniqueName() + "@email.de";
    }

    @Test
    public void testSuccess() throws IOException, InterruptedException {
        long uniq = System.currentTimeMillis();
        registerUser("ab", "b", "correct" + uniq + "@email.de", TEST_PASSWORD);
        assertSuccessfullRegMail("correct" + uniq + "@email.de");

        String defaultSignup = "fname=" + URLEncoder.encode("ab", "UTF-8") + "&lname=" + URLEncoder.encode("b", "UTF-8") + "&pword1=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&pword2=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&day=1&month=1&year=1910&tos_agree=1&dp_agree=1&mname=mn&suffix=sf&email=";

        String query = defaultSignup + URLEncoder.encode("correct3_" + uniq + "@email.de", "UTF-8") + "&name-type=western";
        String data = fetchStartErrorMessage(runRegister(query));
        assertNull(data);
        assertSuccessfullRegMail("correct3_" + uniq + "@email.de");

        getMailReceiver().setEmailCheckError("400 Greylisted");
        getMailReceiver().setApproveRegex(Pattern.compile("a"));
        query = defaultSignup + URLEncoder.encode("correct4_" + uniq + "@email.de", "UTF-8");
        data = fetchStartErrorMessage(runRegister(query));
        assertNotNull(data);

        assertNull(getMailReceiver().poll(null));

    }

    private void assertSuccessfullRegMail(String mail) {
        String link = getMailReceiver().receive(mail).extractLink();
        assertTrue(link, link.startsWith("https://"));
    }

    @Test
    public void testNoFname() throws IOException {
        testFailedForm("lname=b" + createBase() + "&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testNoLname() throws IOException {
        testFailedForm("fname=a" + createBase() + "&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testNoEmail() throws IOException {
        testFailedForm("fname=a&lname=b&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b&email=e@&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b&email=@d.ef&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testNoPword() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword2=ap&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testDiffPword() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "2&day=1&month=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testNoDay() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&month=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testNoMonth() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testNoYear() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testInvDay() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&day=40&month=1&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=0&month=1&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=-1&month=1&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=a&month=1&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testInvMonth() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=20&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=0&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=-1&year=1910&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=a&year=1910&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testInvYear() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=0&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=100&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=a&tos_agree=1&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=-1&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testNoTosAgree() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=1910&tos_agree=a&dp_agree=1");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=1910&dp_agree=1");
    }

    @Test
    public void testNoDPAgree() throws IOException {
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=1910&tos_agree=1&dp_agree=a");
        testFailedForm("fname=a&lname=b" + createBase() + "&day=1&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testTooYoung() throws IOException {
        Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.YEAR, -User.MINIMUM_AGE + 2);
        testFailedForm("fname=a&lname=b&email=" + createUniqueName() + "@email.de" + p + "&day=" + c.get(Calendar.DAY_OF_MONTH) + "&month=" + (c.get(Calendar.MONTH) + 1) + "&year=" + c.get(Calendar.YEAR) + "&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testTooOld() throws IOException {
        Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.YEAR, -User.MAXIMUM_PLAUSIBLE_AGE);
        c.add(Calendar.DAY_OF_MONTH, -1);
        testFailedForm("fname=a&lname=b&email=" + createUniqueName() + "@email.de" + p + "&day=" + c.get(Calendar.DAY_OF_MONTH) + "&month=" + (c.get(Calendar.MONTH) + 1) + "&year=" + c.get(Calendar.YEAR) + "&tos_agree=1&dp_agree=1");
    }

    @Test
    public void testDataStays() throws IOException {
        long uniq = System.currentTimeMillis();
        String run = runRegister("fname=fn" + uniq + "&lname=ln" + uniq + "&email=ma" + uniq + "@example.com&pword1=pas" + uniq + "&pword2=pas2" + uniq + "&day=28&month=10&year=1950");
        assertThat(run, containsString("fn" + uniq));
        assertThat(run, containsString("ln" + uniq));
        assertThat(run, containsString("ma" + uniq + "@example.com"));
        assertThat(run, not(containsString("pas" + uniq)));
        assertThat(run, not(containsString("pas2" + uniq)));
        // test year
        assertThat(run, containsString("name=\"year\" value=\"1950\""));
        // test month
        assertThat(run, containsString("<option value='10' selected=\"selected\">O"));
        // test day
        assertThat(run, containsString("<option selected=\"selected\">28</option>"));
    }

    @Test
    public void testDoubleMail() throws IOException {
        long uniq = System.currentTimeMillis();
        registerUser("RegisterTest", "User", "testmail" + uniq + "@example.com", TEST_PASSWORD);
        getMailReceiver().receive("testmail" + uniq + "@example.com");
        try {
            registerUser("RegisterTest", "User", "testmail" + uniq + "@example.com", TEST_PASSWORD);
            throw new Error("Registering a user with the same email needs to fail.");
        } catch (AssertionError e) {

        }
    }

    @Test
    public void testInvalidMailbox() {
        getMailReceiver().setApproveRegex(Pattern.compile("a"));
        long uniq = System.currentTimeMillis();
        try {
            registerUser("RegisterTest", "User", "testInvalidMailbox" + uniq + "@example.com", TEST_PASSWORD);
            throw new Error("Registering a user with invalid mailbox must fail.");
        } catch (AssertionError e) {

        }
    }

    private void testFailedForm(String query) throws IOException {
        String startError = fetchStartErrorMessage(runRegister(query));
        assertTrue(startError, !startError.startsWith("</div>"));
    }

    @Test
    public void testRegisterWithCountry() throws IOException, InterruptedException {
        long uniq = System.currentTimeMillis();
        String email = "country" + uniq + "@email.de";

        String defaultSignup = "fname=" + URLEncoder.encode("ab", "UTF-8") + "&lname=" + URLEncoder.encode("b", "UTF-8") + "&pword1=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&pword2=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&day=1&month=1&year=1910&tos_agree=1&dp_agree=1&mname=mn&suffix=sf&email=";

        String query = defaultSignup + URLEncoder.encode(email, "UTF-8") + "&name-type=western&residenceCountry=DE";
        String data = fetchStartErrorMessage(runRegister(query));
        assertNull(data);
        User u = User.getByEmail(email);
        assertEquals("DE", u.getResidenceCountry().getCode());
        getMailReceiver().receive(email);
    }

    @Test
    public void testRegisterWithoutCountry() throws IOException, InterruptedException {
        long uniq = System.currentTimeMillis();
        String email = "countryno" + uniq + "@email.de";

        String defaultSignup = "fname=" + URLEncoder.encode("ab", "UTF-8") + "&lname=" + URLEncoder.encode("b", "UTF-8") + "&pword1=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&pword2=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&day=1&month=1&year=1910&tos_agree=1&dp_agree=1&mname=mn&suffix=sf&email=";

        String query = defaultSignup + URLEncoder.encode(email, "UTF-8") + "&name-type=western&residenceCountry=invalid";
        String data = fetchStartErrorMessage(runRegister(query));
        assertNull(data);
        User u = User.getByEmail(email);
        assertEquals(null, u.getResidenceCountry());
        getMailReceiver().receive(email);
    }
}
