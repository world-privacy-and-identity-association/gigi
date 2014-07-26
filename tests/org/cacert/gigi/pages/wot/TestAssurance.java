package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestAssurance extends ManagedTest {

    private String assurerM;

    private String assureeM;

    private int assurer;

    private int assuree;

    private String cookie;

    @Before
    public void setup() throws IOException {
        assurerM = createUniqueName() + "@cacert-test.org";
        assureeM = createUniqueName() + "@cacert-test.org";
        assurer = createAssuranceUser("a", "b", assurerM, TEST_PASSWORD);
        assuree = createAssuranceUser("a", "c", assureeM, TEST_PASSWORD);
        cookie = login(assurerM, TEST_PASSWORD);

    }

    @Test
    public void testAssureSearch() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910");
        assertTrue(loc, loc.endsWith(AssurePage.PATH + "/" + assuree));
    }

    @Test
    public void testAssureSearchEmail() throws IOException {
        String loc = search("email=1" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1910");
        assertNull(loc);
    }

    @Test
    public void testAssureSearchDob() throws IOException {
        String loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=2&month=1&year=1910");
        assertNull(loc);
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=2&year=1910");
        assertNull(loc);
        loc = search("email=" + URLEncoder.encode(assureeM, "UTF-8") + "&day=1&month=1&year=1911");
        assertNull(loc);
    }

    private String search(String query) throws MalformedURLException, IOException, UnsupportedEncodingException {
        URL u = new URL("https://" + getServerName() + AssurePage.PATH);
        URLConnection uc = u.openConnection();
        uc.setDoOutput(true);
        uc.addRequestProperty("Cookie", cookie);
        uc.getOutputStream().write((query).getBytes());
        uc.getOutputStream().flush();

        String loc = uc.getHeaderField("Location");
        return loc;
    }

    @Test
    public void testAssureForm() throws IOException {
        String error = getError("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10");
        assertTrue(error, error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormNoCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10").getBytes());
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormWrongCSRF() throws IOException {
        // override csrf
        HttpURLConnection uc = (HttpURLConnection) buildupAssureFormConnection(false);
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10&csrf=aragc").getBytes());
        uc.getOutputStream().flush();
        assertEquals(500, uc.getResponseCode());
    }

    @Test
    public void testAssureFormRace() throws IOException, SQLException {
        URLConnection uc = buildupAssureFormConnection(true);
        PreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE `users` SET email='changed' WHERE id=?");
        ps.setInt(1, assuree);
        ps.execute();
        uc.getOutputStream().write(("date=2000-01-01&location=testcase&certify=1&rules=1&CCAAgreed=1&assertion=1&points=10").getBytes());
        uc.getOutputStream().flush();
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        assertTrue(error, !error.startsWith("</div>"));
    }

    @Test
    public void testAssureFormFuture() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY");
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

    private String getError(String query) throws MalformedURLException, IOException {
        URLConnection uc = buildupAssureFormConnection(true);
        uc.getOutputStream().write((query).getBytes());
        uc.getOutputStream().flush();
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        return error;
    }

    private URLConnection buildupAssureFormConnection(boolean doCSRF) throws MalformedURLException, IOException {
        URL u = new URL("https://" + getServerName() + AssurePage.PATH + "/" + assuree);
        URLConnection uc = u.openConnection();
        uc.addRequestProperty("Cookie", cookie);
        String csrf = getCSRF(uc);
        uc = u.openConnection();
        uc.addRequestProperty("Cookie", cookie);
        uc.setDoOutput(true);
        if (doCSRF) {
            uc.getOutputStream().write(("csrf=" + csrf + "&").getBytes());
        }
        return uc;
    }

}
