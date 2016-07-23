package org.cacert.gigi.pages.wot;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.util.DayDate;
import org.cacert.gigi.util.Notary;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class TestAssurance extends ManagedTest {

    private String assurerM;

    private String assureeM;

    private int assureeName;

    private String cookie;

    @Before
    public void setup() throws IOException {
        clearCaches();
        assurerM = createUniqueName() + "@cacert-test.org";
        assureeM = createUniqueName() + "@cacert-test.org";

        createAssuranceUser("a", "b", assurerM, TEST_PASSWORD);
        int assureeId = createVerifiedUser("a", "c", assureeM, TEST_PASSWORD);
        assureeName = User.getById(assureeId).getPreferredName().getId();

        cookie = login(assurerM, TEST_PASSWORD);
    }

    private Matcher<String> isAssuranceForm() {
        return containsString("<select name=\"assuranceType\">");
    }

    @Test
    public void testAssureSearch() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910");
        assertThat(loc, isAssuranceForm());
    }

    @Test
    public void testAssureSearchEmail() throws IOException {
        String loc = search("email=1" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910");
        assertThat(loc, not(isAssuranceForm()));
    }

    @Test
    public void testAssureSearchDobInvalid() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=mal");
        assertThat(loc, not(isAssuranceForm()));
    }

    @Test
    public void testAssureSearchDob() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=2&month=1&year=1910");
        assertThat(loc, not(isAssuranceForm()));
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=2&year=1910");
        assertThat(loc, not(isAssuranceForm()));
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1911");
        assertThat(loc, not(isAssuranceForm()));
    }

    private String search(String query) throws MalformedURLException, IOException, UnsupportedEncodingException {
        URLConnection uc = get(cookie, AssurePage.PATH);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("search&" + query).getBytes("UTF-8"));
        uc.getOutputStream().flush();

        return IOUtils.readURL(uc);
    }

    @Test
    public void testAssureForm() throws IOException {
        executeSuccess("date=" + validVerificationDateString() + "&location=testcase&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormContanisData() throws IOException {
        URLConnection uc = buildupAssureFormConnection(true);
        uc.getOutputStream().write(("assuredName=" + assureeName + "&date=" + validVerificationDateString() + "&location=testcase&rules=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String data = IOUtils.readURL(uc);
        assertThat(data, containsString(validVerificationDateString()));
        assertThat(data, containsString("testcase"));
    }

    @Test
    public void testAssureFormNoCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=" + validVerificationDateString() + "&location=testcase&certify=1&rules=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormWrongCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=" + validVerificationDateString() + "&location=testcase&certify=1&rules=1&assertion=1&points=10&csrf=aragc").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormRaceDoB() throws IOException, SQLException {
        testAssureFormRace(false);
    }

    @Test
    public void testAssureFormRaceDoBBlind() throws IOException, SQLException {
        testAssureFormRace(true);
    }

    public void testAssureFormRace(boolean succeed) throws IOException, SQLException {
        URLConnection uc = buildupAssureFormConnection(true);

        String assureeCookie = login(assureeM, TEST_PASSWORD);
        String newDob = "day=1&month=1&year=" + ( !succeed ? 1911 : 1910);

        assertNull(executeBasicWebInteraction(assureeCookie, MyDetails.PATH, newDob + "&action=updateDoB", 0));

        uc.getOutputStream().write(("assuredName=" + assureeName + "&date=" + validVerificationDateString() + "&location=testcase&certify=1&rules=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        if (succeed) {
            assertNull(error);
        } else {
            assertTrue(error, !error.startsWith("</div>"));
            assertThat(error, containsString("changed his personal details"));
        }
    }

    @Test
    public void testAssureFormFuture() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        int year = Integer.parseInt(sdf.format(new Date(System.currentTimeMillis()))) + 2;
        executeFails("date=" + year + "-01-01&location=testcase&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormFutureOK() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.HOUR_OF_DAY, 12);

        executeSuccess("date=" + sdf.format(new Date(c.getTimeInMillis())) + "&location=testcase&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormPastInRange() throws IOException {
        executeSuccess("date=" + validVerificationDateString() + "&location=testcase&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormPastOnLimit() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -Notary.LIMIT_MAX_MONTHS_VERIFICATION);
        c.add(Calendar.DAY_OF_MONTH, 1);

        executeSuccess("date=" + sdf.format(new Date(c.getTimeInMillis())) + "&location=testcase&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormPastOutOfRange() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -Notary.LIMIT_MAX_MONTHS_VERIFICATION);

        executeFails("date=" + sdf.format(new Date(c.getTimeInMillis())) + "&location=testcase&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormNoLoc() throws IOException {
        executeFails("date=" + validVerificationDateString() + "&location=a&certify=1&rules=1&assertion=1&points=10");
        executeFails("date=" + validVerificationDateString() + "&location=&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormInvalDate() throws IOException {
        executeFails("date=20000101&location=testcase&certify=1&rules=1&assertion=1&points=10");
        executeFails("date=&location=testcase&certify=1&rules=1&assertion=1&points=10");
    }

    @Test
    public void testAssureFormBoxes() throws IOException {
        executeFails("date=" + validVerificationDateString() + "&location=testcase&certify=0&rules=1&assertion=1&points=10");
        executeFails("date=" + validVerificationDateString() + "&location=testcase&certify=1&rules=&assertion=1&points=10");
        executeFails("date=" + validVerificationDateString() + "&location=testcase&certify=1&rules=1&assertion=z&points=10");
    }

    @Test
    public void testAssureListingValid() throws IOException {
        String uniqueLoc = createUniqueName();
        execute("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&certify=1&rules=1&assertion=1&points=10");

        String cookie = login(assureeM, TEST_PASSWORD);
        URLConnection url = get(cookie, MyPoints.PATH);
        String resp = IOUtils.readURL(url);
        resp = resp.split(Pattern.quote("</table>"))[0];
        assertThat(resp, containsString(uniqueLoc));
    }

    @Test
    public void testAssurerListingValid() throws IOException {
        String uniqueLoc = createUniqueName();
        executeSuccess("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&certify=1&rules=1&assertion=1&points=10");
        String cookie = login(assurerM, TEST_PASSWORD);
        URLConnection url = get(cookie, MyPoints.PATH);
        String resp = IOUtils.readURL(url);
        resp = resp.split(Pattern.quote("</table>"))[1];
        assertThat(resp, containsString(uniqueLoc));
    }

    private void executeFails(String query) throws MalformedURLException, IOException {
        assertThat(execute(query), hasError());

    }

    private void executeSuccess(String query) throws MalformedURLException, IOException {
        assertThat(execute(query), hasNoError());

    }

    private String execute(String query) throws MalformedURLException, IOException {
        URLConnection uc = buildupAssureFormConnection(true);
        uc.getOutputStream().write(("assuredName=" + assureeName + "&" + query).getBytes("UTF-8"));
        uc.getOutputStream().flush();
        return IOUtils.readURL(uc);
    }

    private URLConnection buildupAssureFormConnection(boolean doCSRF) throws MalformedURLException, IOException {
        return buildupAssureFormConnection(cookie, assureeM, doCSRF);
    }

    public static URLConnection buildupAssureFormConnection(String cookie, String email, boolean doCSRF) throws MalformedURLException, IOException {
        URLConnection uc = get(cookie, AssurePage.PATH);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("email=" + URLEncoder.encode(email, "UTF-8") + "&day=1&month=1&year=1910&search").getBytes("UTF-8"));

        String csrf = getCSRF(uc);
        uc = get(cookie, AssurePage.PATH);
        uc.setDoOutput(true);
        if (doCSRF) {
            uc.getOutputStream().write(("csrf=" + csrf + "&").getBytes("UTF-8"));
        }
        return uc;
    }

    @Test
    public void testMultipleAssurance() throws IOException {

        User users[] = User.findByEmail(assurerM);
        int agentID = users[0].getId();

        users = User.findByEmail(assureeM);
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
        executeSuccess("date=" + validVerificationDateString() + "&location=" + uniqueLoc + "&certify=1&rules=1&assertion=1&points=10");

        // enter third entry on the same day
        URLConnection uc = get(cookie, AssurePage.PATH);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910&search").getBytes("UTF-8"));
        assertThat(IOUtils.readURL(uc), hasError());

    }
}
