package club.wpia.gigi.pages.wot;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.CATS.CATSType;
import club.wpia.gigi.dbObjects.Country;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.account.MyDetails;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.ManagedTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.DayDate;
import club.wpia.gigi.util.Notary;

public class TestVerification extends ManagedTest {

    private String agentM;

    private String applicantM;

    private int applicantName;

    private int applicantId;

    private String cookie;

    @Before
    public void setup() throws IOException, GeneralSecurityException, GigiApiException, InterruptedException {
        clearCaches();
        agentM = createUniqueName() + "@example.org";
        applicantM = createUniqueName() + "@example.org";

        createVerificationUser("a", "b", agentM, TEST_PASSWORD);
        applicantId = createVerifiedUser("a", "c", applicantM, TEST_PASSWORD);
        applicantName = User.getById(applicantId).getPreferredName().getId();

        User users[] = User.findByEmail(agentM);
        cookie = cookieWithCertificateLogin(users[0]);
    }

    private Matcher<String> isVerificationForm() {
        return containsString("<select name=\"verificationType\">");
    }

    @Test
    public void testVerifySearch() throws IOException {
        String loc = search("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=1&year=1910");
        assertThat(loc, isVerificationForm());
    }

    @Test
    public void testVerifySearchEmail() throws IOException {
        String loc = search("email=1" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=1&year=1910");
        assertThat(loc, not(isVerificationForm()));
    }

    @Test
    public void testVerifySearchDobInvalid() throws IOException {
        String loc = search("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=1&year=mal");
        assertThat(loc, not(isVerificationForm()));
    }

    @Test
    public void testVerifySearchDob() throws IOException {
        String loc = search("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=2&month=1&year=1910");
        assertThat(loc, not(isVerificationForm()));
        loc = search("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=2&year=1910");
        assertThat(loc, not(isVerificationForm()));
        loc = search("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=1&year=1911");
        assertThat(loc, not(isVerificationForm()));
    }

    private String search(String query) throws MalformedURLException, IOException, UnsupportedEncodingException {
        URLConnection uc = get(cookie, VerifyPage.PATH);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("search&" + query).getBytes("UTF-8"));
        uc.getOutputStream().flush();

        return IOUtils.readURL(uc);
    }

    @Test
    public void testVerifyForm() throws IOException {
        String body = executeSuccess("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        assertThat(body, containsString("10"));
        assertThat(body, containsString(applicantM));
        getMailReceiver().receive(applicantM);
    }

    @Test
    public void testVerifyFormEmpty() throws IOException {
        URLConnection uc = buildupVerifyFormConnection(true);
        uc.getOutputStream().write(("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&rules=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String data = IOUtils.readURL(uc);
        assertThat(data, hasError());
    }

    @Test
    public void testVerifyFormContainsData() throws IOException {
        URLConnection uc = buildupVerifyFormConnection(true);
        uc.getOutputStream().write(("verifiedName=" + applicantName + "&date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&rules=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String data = IOUtils.readURL(uc);
        assertThat(data, containsString(validVerificationDateString()));
        assertThat(data, containsString("testcase"));
    }

    @Test
    public void testVerifyFormNoCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupVerifyFormConnection(false);
        uc.getOutputStream().write(("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
        uc.getErrorStream().close();
    }

    @Test
    public void testVerifyFormWrongCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupVerifyFormConnection(false);
        uc.getOutputStream().write(("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10&csrf=aragc").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
        uc.getErrorStream().close();
    }

    @Test
    public void testVerifyFormRaceDoB() throws IOException, SQLException {
        testVerifyFormRace(false);
    }

    @Test
    public void testVerifyFormRaceDoBBlind() throws IOException, SQLException {
        testVerifyFormRace(true);
    }

    public void testVerifyFormRace(boolean succeed) throws IOException, SQLException {
        URLConnection uc = buildupVerifyFormConnection(true);

        String applicantCookie = login(applicantM, TEST_PASSWORD);
        String newDob = "day=1&month=1&year=" + ( !succeed ? 1911 : 1910);
        loginCertificate = null;
        assertNull(executeBasicWebInteraction(applicantCookie, MyDetails.PATH, newDob + "&action=updateDoB", 0));

        uc.getOutputStream().write(("verifiedName=" + applicantName + "&date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        if (succeed) {
            assertNull(error);
            getMailReceiver().receive(applicantM);
        } else {
            assertTrue(error, !error.startsWith("</div>"));
            assertThat(error, containsString("changed his personal details"));
        }
    }

    @Test
    public void testVerifyFormFuture() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        int year = Integer.parseInt(sdf.format(new Date(System.currentTimeMillis()))) + 2;
        executeFails("date=" + year + "-01-01&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testVerifyFormFutureOK() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.HOUR_OF_DAY, 12);

        executeSuccess("date=" + sdf.format(new Date(c.getTimeInMillis())) + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        getMailReceiver().receive(applicantM);
    }

    @Test
    public void testVerifyFormPastInRange() throws IOException {
        executeSuccess("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        getMailReceiver().receive(applicantM);
    }

    @Test
    public void testVerifyFormPastOnLimit() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -Notary.LIMIT_MAX_MONTHS_VERIFICATION);
        c.add(Calendar.DAY_OF_MONTH, 1);

        executeSuccess("date=" + sdf.format(new Date(c.getTimeInMillis())) + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        getMailReceiver().receive(applicantM);
    }

    @Test
    public void testVerifyFormPastOutOfRange() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -Notary.LIMIT_MAX_MONTHS_VERIFICATION);

        executeFails("date=" + sdf.format(new Date(c.getTimeInMillis())) + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testVerifyFormNoLoc() throws IOException {
        executeFails("date=" + validVerificationDateString() + "&location=a&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        executeFails("date=" + validVerificationDateString() + "&location=&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testVerifyFormInvalDate() throws IOException {
        executeFails("date=20000101&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        executeFails("date=&location=testcase&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testVerifyFormBoxes() throws IOException {
        executeFails("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=0&rules=1&assertion=1&points=10");
        executeFails("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=&assertion=1&points=10");
        executeFails("date=" + validVerificationDateString() + "&location=testcase&countryCode=DE&certify=1&rules=1&assertion=z&points=10");
    }

    @Test
    public void testVerifyListingValid() throws IOException, GigiApiException {
        String uniqueLoc = createUniqueName();
        executeSuccess("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        getMailReceiver().receive(applicantM);

        String cookie = login(applicantM, TEST_PASSWORD);
        loginCertificate = null;
        URLConnection url = get(cookie, Points.PATH);
        String resp = IOUtils.readURL(url);
        resp = resp.split(Pattern.quote("</table>"))[1];
        assertThat(resp, containsString(uniqueLoc));
        assertThat(resp, containsString(Country.getCountryByCode("DE", Country.CountryCodeType.CODE_2_CHARS).getName()));
    }

    @Test
    public void testAgentListingValid() throws IOException, GigiApiException {
        String uniqueLoc = createUniqueName();
        executeSuccess("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        getMailReceiver().receive(applicantM);

        String cookie = login(agentM, TEST_PASSWORD);
        loginCertificate = null;
        URLConnection url = get(cookie, Points.PATH);
        String resp = IOUtils.readURL(url);
        resp = resp.split(Pattern.quote("</table>"))[2];
        assertThat(resp, containsString(uniqueLoc));
        assertThat(resp, containsString(Country.getCountryByCode("DE", Country.CountryCodeType.CODE_2_CHARS).getName()));
    }

    private void executeFails(String query) throws MalformedURLException, IOException {
        assertThat(execute(query), hasError());

    }

    private String executeSuccess(String query) throws MalformedURLException, IOException {
        String response = execute(query);
        assertThat(response, hasNoError());
        return response;
    }

    private String execute(String query) throws MalformedURLException, IOException {
        URLConnection uc = buildupVerifyFormConnection(true);
        uc.getOutputStream().write(("verifiedName=" + applicantName + "&" + query).getBytes("UTF-8"));
        uc.getOutputStream().flush();
        return IOUtils.readURL(uc);
    }

    private URLConnection buildupVerifyFormConnection(boolean doCSRF) throws MalformedURLException, IOException {
        return buildupVerifyFormConnection(cookie, applicantM, doCSRF);
    }

    public static URLConnection buildupVerifyFormConnection(String cookie, String email, boolean doCSRF) throws MalformedURLException, IOException {
        URLConnection uc = get(cookie, VerifyPage.PATH);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("email=" + URLEncoder.encode(email, "UTF-8") + "&day=1&month=1&year=1910&search").getBytes("UTF-8"));

        String csrf = getCSRF(uc);
        uc = get(cookie, VerifyPage.PATH);
        uc.setDoOutput(true);
        if (doCSRF) {
            uc.getOutputStream().write(("csrf=" + csrf + "&").getBytes("UTF-8"));
        }
        return uc;
    }

    @Test
    public void testMultipleVerification() throws IOException, GeneralSecurityException, GigiApiException, InterruptedException {
        User users[] = User.findByEmail(agentM);
        int agentID = users[0].getId();

        users = User.findByEmail(applicantM);
        int applicantID = users[0].getId();

        // enter first entry 200 days in the past
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?, `when`=? ")) {
            ps.setInt(1, agentID);
            ps.setInt(2, applicantID);
            ps.setInt(3, 10);
            ps.setString(4, "test-location");
            ps.setString(5, "2010-01-01");
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis() - DayDate.MILLI_DAY * 200));
            ps.execute();
        }

        // enter second entry
        String uniqueLoc = createUniqueName();
        executeSuccess("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        getMailReceiver().receive(applicantM);

        // enter third entry on the same day
        URLConnection uc = get(cookie, VerifyPage.PATH);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=1&year=1910&search").getBytes("UTF-8"));
        assertThat(IOUtils.readURL(uc), hasError());

    }

    @Test
    public void testVerifyFormNoCountry() throws IOException {
        executeFails("date=" + validVerificationDateString() + "&location=testcase&countryCode=&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testRANotificationSet() throws IOException, GigiApiException, GeneralSecurityException, InterruptedException {
        getMailReceiver().assertEmpty();

        User users[] = User.findByEmail(agentM);
        assertTrue("user RA Agent not found", users != null && users.length > 0);

        User u = users[0];
        u.grantGroup(u, Group.VERIFY_NOTIFICATION);
        clearCaches();
        cookie = cookieWithCertificateLogin(users[0]);

        // enter verification
        String uniqueLoc = createUniqueName();
        executeSuccess("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&countryCode=DE&certify=1&rules=1&assertion=1&points=10");
        getMailReceiver().receive(applicantM);

        TestMail tm = getMailReceiver().receive(agentM);
        assertThat(tm.getMessage(), containsString("You entered a verification for the account with email address " + applicantM));

    }

    @Test
    public void testRANotificationNotSet() throws IOException, GigiApiException {
        getMailReceiver().assertEmpty();

        User users[] = User.findByEmail(agentM);
        assertTrue("user RA Agent not found", users != null && users.length > 0);

        User u = users[0];
        u.revokeGroup(u, Group.VERIFY_NOTIFICATION);
        clearCaches();

        // enter verification
        String uniqueLoc = createUniqueName();
        executeSuccess("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&countryCode=DE&certify=1&rules=1&assertion=1&points=10");

        TestMail tm = getMailReceiver().receive(applicantM);
        assertThat(tm.getMessage(), not(containsString("You entered a verification for the account with email address " + applicantM)));

    }

    @Test
    public void testVerifyWithoutCertLogin() throws IOException {
        cookie = login(agentM, TEST_PASSWORD);
        loginCertificate = null;
        assertEquals(403, get(cookie, VerifyPage.PATH).getResponseCode());
    }

    @Test
    public void testVerifyWithoutValidChallenge() throws IOException, GigiApiException {
        cookie = cookieWithCertificateLogin(User.getById(applicantId));
        add100Points(applicantId);
        insertRAContract(applicantId);
        addChallengeInPast(applicantId, CATSType.AGENT_CHALLENGE);
        assertEquals(403, get(cookie, VerifyPage.PATH).getResponseCode());
        addChallenge(applicantId, CATSType.AGENT_CHALLENGE);
        assertEquals(200, get(cookie, VerifyPage.PATH).getResponseCode());
    }

    @Test
    public void testVerifyValidTTPChallenge() throws IOException, GigiApiException {
        grant(User.getByEmail(agentM), Group.TTP_AGENT);
        grant(User.getById(applicantId), Group.TTP_APPLICANT);
        cookie = cookieWithCertificateLogin(User.getById(applicantId));
        cookie = cookieWithCertificateLogin(User.getByEmail(agentM));

        // test without valid challenge
        String content = search("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=1&year=1910");
        assertThat(content, containsString("you need to pass the TTP RA Agent Challenge"));

        // test with valid challenge
        addChallenge(User.getByEmail(agentM).getId(), CATSType.TTP_AGENT_CHALLENGE);
        content = search("email=" + URLEncoder.encode(applicantM, "UTF-8") + "&day=1&month=1&year=1910");
        assertThat(content, not(containsString("you need to pass the TTP RA Agent Challenge")));
    }
}
