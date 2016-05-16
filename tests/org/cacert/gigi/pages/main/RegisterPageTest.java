package org.cacert.gigi.pages.main;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import org.cacert.gigi.testUtils.InitTruststore;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Before;
import org.junit.Test;

public class RegisterPageTest extends ManagedTest {

    static {
        InitTruststore.run();
        HttpURLConnection.setFollowRedirects(false);
    }

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testSuccess() throws IOException, InterruptedException {
        long uniq = System.currentTimeMillis();
        registerUser("ab", "b", "correct" + uniq + "@email.de", TEST_PASSWORD);
        assertSuccessfullRegMail();

        String defaultSignup = "fname=" + URLEncoder.encode("ab", "UTF-8") + "&lname=" + URLEncoder.encode("b", "UTF-8") + "&pword1=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&pword2=" + URLEncoder.encode(TEST_PASSWORD, "UTF-8") + "&day=1&month=1&year=1910&tos_agree=1&mname=mn&suffix=sf&email=";

        String query = defaultSignup + URLEncoder.encode("correct3_" + uniq + "@email.de", "UTF-8") + "&general=1&country=1&regional=1&radius=1";
        String data = fetchStartErrorMessage(runRegister(query));
        assertNull(data);
        assertSuccessfullRegMail();

        getMailReciever().setEmailCheckError("400 Greylisted");
        getMailReciever().setApproveRegex(Pattern.compile("a"));
        query = defaultSignup + URLEncoder.encode("correct4_" + uniq + "@email.de", "UTF-8") + "&general=1&country=1&regional=1&radius=1";
        data = fetchStartErrorMessage(runRegister(query));
        assertNotNull(data);

        assertNull(getMailReciever().poll());

    }

    private void assertSuccessfullRegMail() {
        String link = getMailReciever().receive().extractLink();
        assertTrue(link, link.startsWith("https://"));
    }

    @Test
    public void testNoFname() throws IOException {
        testFailedForm("lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testNoLname() throws IOException {
        testFailedForm("fname=a&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testNoEmail() throws IOException {
        testFailedForm("fname=a&lname=b&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testNoPword() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword2=ap&day=1&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testDiffPword() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap2&day=1&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testNoDay() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testNoMonth() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&year=1910&tos_agree=1");
    }

    @Test
    public void testNoYear() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&tos_agree=1");
    }

    @Test
    public void testInvDay() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=40&month=1&year=1910&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=0&month=1&year=1910&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=-1&month=1&year=1910&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=a&month=1&year=1910&tos_agree=1");
    }

    @Test
    public void testInvMonth() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=20&year=1910&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=0&year=1910&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=-1&year=1910&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=a&year=1910&tos_agree=1");
    }

    @Test
    public void testInvYear() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=0&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=100&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=a&tos_agree=1");
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=-1&tos_agree=1");
    }

    @Test
    public void testNoAgree() throws IOException {
        testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&tos_agree=a");
    }

    @Test
    public void testDataStays() throws IOException {
        long uniq = System.currentTimeMillis();
        String run = runRegister("fname=fn" + uniq + "&lname=ln" + uniq + "&email=ma" + uniq + "@cacert.org&pword1=pas" + uniq + "&pword2=pas2" + uniq + "&day=1&month=1&year=0");
        assertThat(run, containsString("fn" + uniq));
        assertThat(run, containsString("ln" + uniq));
        assertThat(run, containsString("ma" + uniq + "@cacert.org"));
        assertThat(run, not(containsString("pas" + uniq)));
        assertThat(run, not(containsString("pas2" + uniq)));

    }

    @Test
    public void testCheckboxesStay() throws IOException {
        String run2 = runRegister("general=1&country=a&regional=1&radius=0");
        assertThat(run2, containsString("name=\"general\" value=\"1\" checked=\"checked\">"));
        assertThat(run2, containsString("name=\"country\" value=\"1\">"));
        assertThat(run2, containsString("name=\"regional\" value=\"1\" checked=\"checked\">"));
        assertThat(run2, containsString("name=\"radius\" value=\"1\">"));
        run2 = runRegister("general=0&country=1&radius=1");
        assertThat(run2, containsString("name=\"general\" value=\"1\">"));
        assertThat(run2, containsString("name=\"country\" value=\"1\" checked=\"checked\">"));
        assertThat(run2, containsString("name=\"regional\" value=\"1\">"));
        assertThat(run2, containsString("name=\"radius\" value=\"1\" checked=\"checked\">"));
    }

    @Test
    public void testDoubleMail() throws IOException {
        long uniq = System.currentTimeMillis();
        registerUser("RegisterTest", "User", "testmail" + uniq + "@cacert.org", TEST_PASSWORD);
        try {
            registerUser("RegisterTest", "User", "testmail" + uniq + "@cacert.org", TEST_PASSWORD);
            throw new Error("Registering a user with the same email needs to fail.");
        } catch (AssertionError e) {

        }
    }

    @Test
    public void testInvalidMailbox() {
        getMailReciever().setApproveRegex(Pattern.compile("a"));
        long uniq = System.currentTimeMillis();
        try {
            registerUser("RegisterTest", "User", "testInvalidMailbox" + uniq + "@cacert.org", TEST_PASSWORD);
            throw new Error("Registering a user with invalid mailbox must fail.");
        } catch (AssertionError e) {

        }
    }

    private void testFailedForm(String query) throws IOException {
        String startError = fetchStartErrorMessage(runRegister(query));
        assertTrue(startError, !startError.startsWith("</div>"));
    }

}
