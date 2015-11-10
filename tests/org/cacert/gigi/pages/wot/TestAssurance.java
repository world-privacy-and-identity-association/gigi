package org.cacert.gigi.pages.wot;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
        assertTrue(loc, loc.contains("type=\"checkbox\" name=\"CCAAgreed\""));
    }

    @Test
    public void testAssureSearchEmail() throws IOException {
        String loc = search("email=1" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"CCAAgreed\""));
    }

    @Test
    public void testAssureSearchDob() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=2&month=1&year=1910");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"CCAAgreed\""));
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=2&year=1910");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"CCAAgreed\""));
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1911");
        assertTrue(loc, !loc.contains("type=\"checkbox\" name=\"CCAAgreed\""));
    }

    private String search(String query) throws MalformedURLException, IOException, UnsupportedEncodingException {
        URL u = new URL("https://" + getServerName() + AssurePage.PATH);
        URLConnection uc = u.openConnection();
        uc.setDoOutput(true);
        uc.addRequestProperty("Cookie", cookie);
        uc.getOutputStream().write(("search&" + query).getBytes("UTF-8"));
        uc.getOutputStream().flush();

        return IOUtils.readURL(uc);
    }

    @Test
    public void testAssureForm() throws IOException {
        String error = getError("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertNull(error);
    }

    @Test
    public void testAssureFormContanisData() throws IOException {
        URLConnection uc = buildupAssureFormConnection(true);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&rules=1&CCAAgreed=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String data = IOUtils.readURL(uc);
        assertThat(data, containsString("2000-01-01"));
        assertThat(data, containsString("testcase"));
    }

    @Test
    public void testAssureFormNoCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormWrongCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10&csrf=aragc").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormRaceName() throws IOException, SQLException {
        testAssureFormRace(true);
    }

    @Test
    public void testAssureFormRaceDoB() throws IOException, SQLException {
        testAssureFormRace(false);
    }

    public void testAssureFormRace(boolean name) throws IOException, SQLException {
        URLConnection uc = buildupAssureFormConnection(true);

        String assureeCookie = login(assureeM, TEST_PASSWORD);
        String newName = "lname=" + (name ? "c" : "a") + "&fname=a&mname=&suffix=";
        String newDob = "day=1&month=1&year=" + (name ? 1910 : 1911);

        assertNull(executeBasicWebInteraction(assureeCookie, MyDetails.PATH, newName + "&" + newDob + "&processDetails", 0));

        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10").getBytes("UTF-8"));
        uc.getOutputStream().flush();
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormFuture() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        int year = Integer.parseInt(sdf.format(new Date(System.currentTimeMillis()))) + 2;
        String error = getError("date=" + year + "-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormNoLoc() throws IOException {
        String error = getError("date=2000-01-01&location=a&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormInvalDate() throws IOException {
        String error = getError("date=20000101&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormBoxes() throws IOException {
        String error = getError("date=2000-01-01&location=testcase&certify=0&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=testcase&certify=1&rules=&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=a&assertion=1&points=10");
        assertTrue(error, !error.startsWith("</div>"));
        error = getError("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=z&points=10");
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureListingValid() throws IOException {
        String uniqueLoc = createUniqueName();
        String error = getError("date=2000-01-01&location=" + uniqueLoc + "&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertNull(error);
        String cookie = login(assureeM, TEST_PASSWORD);
        URLConnection url = new URL("https://" + getServerName() + MyPoints.PATH).openConnection();
        url.setRequestProperty("Cookie", cookie);
        String resp = IOUtils.readURL(url);
        resp = resp.split(Pattern.quote("</table>"))[0];
        assertThat(resp, containsString(uniqueLoc));
    }

    @Test
    public void testAssurerListingValid() throws IOException {
        String uniqueLoc = createUniqueName();
        String error = getError("date=2000-01-01&location=" + uniqueLoc + "&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertNull(error);
        String cookie = login(assurerM, TEST_PASSWORD);
        URLConnection url = new URL("https://" + getServerName() + MyPoints.PATH).openConnection();
        url.setRequestProperty("Cookie", cookie);
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
        URL u = new URL("https://" + getServerName() + AssurePage.PATH);
        URLConnection uc = u.openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        uc.getOutputStream().write(("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910&search").getBytes("UTF-8"));

        String csrf = getCSRF(uc);
        uc = u.openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        if (doCSRF) {
            uc.getOutputStream().write(("csrf=" + csrf + "&").getBytes("UTF-8"));
        }
        return uc;
    }

}
