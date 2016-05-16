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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Before;
import org.junit.Test;

public class TestAssurance extends ManagedTest {

    private String assurerM;

    private String assureeM;

    private String cookie;

    @Before
    public void setup() throws IOException {
        assurerM = createUniqueName() + "@cacert-test.org";
        assureeM = createUniqueName() + "@cacert-test.org";

        createAssuranceUser("a", "b", assurerM, TEST_PASSWORD);
        createVerifiedUser("a", "c", assureeM, TEST_PASSWORD);

        cookie = login(assurerM, TEST_PASSWORD);
    }

    @Test
    public void testAssureSearch() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910");
        assertTrue(loc, loc.contains("type=\"checkbox\" name=\"tos_agree\""));
    }

    @Test
    public void testAssureSearchEmail() throws IOException {
        String loc = search("email=1" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"tos_agree\""));
    }

    @Test
    public void testAssureSearchDob() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=2&month=1&year=1910");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"tos_agree\""));
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=2&year=1910");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"tos_agree\""));
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1911");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"tos_agree\""));
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
        String error = getError("date=2000-01-01&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertNull(error);
    }

    @Test
    public void testAssureFormContanisData() throws IOException {
        URLConnection uc = buildupAssureFormConnection(true);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&rules=1&tos_agree=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String data = IOUtils.readURL(uc);
        assertThat(data, containsString("2000-01-01"));
        assertThat(data, containsString("testcase"));
    }

    @Test
    public void testAssureFormNoCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormWrongCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10&csrf=aragc").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormRaceName() throws IOException, SQLException {
        testAssureFormRace(true, false);
    }

    @Test
    public void testAssureFormRaceDoB() throws IOException, SQLException {
        testAssureFormRace(false, false);
    }

    @Test
    public void testAssureFormRaceNameBlind() throws IOException, SQLException {
        testAssureFormRace(true, true);
    }

    @Test
    public void testAssureFormRaceDoBBlind() throws IOException, SQLException {
        testAssureFormRace(false, true);
    }

    public void testAssureFormRace(boolean name, boolean succeed) throws IOException, SQLException {
        URLConnection uc = buildupAssureFormConnection(true);

        String assureeCookie = login(assureeM, TEST_PASSWORD);
        String newName = "lname=" + (name && !succeed ? "a" : "c") + "&fname=a&mname=&suffix=";
        String newDob = "day=1&month=1&year=" + ( !name && !succeed ? 1911 : 1910);

        assertNull(executeBasicWebInteraction(assureeCookie, MyDetails.PATH, newName + "&" + newDob + "&processDetails", 0));

        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10").getBytes("UTF-8"));
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
        String error = getError("date=" + year + "-01-01&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormFutureOK() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.HOUR_OF_DAY, 12);

        String error = getError("date=" + sdf.format(new Date(c.getTimeInMillis())) + "&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertNull(error);
    }

    @Test
    public void testAssureFormNoLoc() throws IOException {
        String error = getError("date=2000-01-01&location=a&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormInvalDate() throws IOException {
        String error = getError("date=20000101&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=&location=testcase&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormBoxes() throws IOException {
        String error = getError("date=2000-01-01&location=testcase&certify=0&rules=1&tos_agree=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=testcase&certify=1&rules=&tos_agree=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=testcase&certify=1&rules=1&tos_agree=a&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=testcase&certify=1&rules=1&tos_agree=1&assertion=z&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureListingValid() throws IOException {
        String uniqueLoc = createUniqueName();
        String error = getError("date=2000-01-01&location=" + uniqueLoc + "&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertNull(error);
        String cookie = login(assureeM, TEST_PASSWORD);
        URLConnection url = get(cookie, MyPoints.PATH);
        String resp = IOUtils.readURL(url);
        resp = resp.split(Pattern.quote("</table>"))[0];
        assertThat(resp, containsString(uniqueLoc));
    }

    @Test
    public void testAssurerListingValid() throws IOException {
        String uniqueLoc = createUniqueName();
        String error = getError("date=2000-01-01&location=" + uniqueLoc + "&certify=1&rules=1&tos_agree=1&assertion=1&points=10");
        assertNull(error);
        String cookie = login(assurerM, TEST_PASSWORD);
        URLConnection url = get(cookie, MyPoints.PATH);
        String resp = IOUtils.readURL(url);
        resp = resp.split(Pattern.quote("</table>"))[1];
        assertThat(resp, containsString(uniqueLoc));
    }

    private String getError(String query) throws MalformedURLException, IOException {
        URLConnection uc = buildupAssureFormConnection(true);
        uc.getOutputStream().write((query).getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        return error;
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

}
