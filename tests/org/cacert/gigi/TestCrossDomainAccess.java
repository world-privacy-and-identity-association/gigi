package org.cacert.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.util.ServerConstants;
import org.junit.Test;

public class TestCrossDomainAccess extends ManagedTest {

    @Test
    public void testNoOriginHeader() throws MalformedURLException, IOException {
        URLConnection con = new URL("https://" + ServerConstants.getWwwHostNamePortSecure() + "/login").openConnection();
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpsToHttps() throws MalformedURLException, IOException {
        URLConnection con = new URL("https://" + ServerConstants.getWwwHostNamePortSecure() + "/login").openConnection();
        con.setRequestProperty("Origin", "https://" + ServerConstants.getWwwHostNamePortSecure());
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpToHttps() throws MalformedURLException, IOException {
        URLConnection con = new URL("https://" + ServerConstants.getWwwHostNamePortSecure() + "/login").openConnection();
        con.setRequestProperty("Origin", "http://" + ServerConstants.getWwwHostNamePort());
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpsToSecure() throws MalformedURLException, IOException, GeneralSecurityException, SQLException, InterruptedException, GigiApiException {
        URLConnection con = new URL("https://" + ServerConstants.getSecureHostNamePort()).openConnection();
        con.setRequestProperty("Origin", "https://" + ServerConstants.getWwwHostNamePortSecure());
        String contains = IOUtils.readURL(con);
        assertTrue( !contains.contains("No cross domain access allowed."));
    }

    @Test
    public void testCorrectOriginHeaderFromHttpsToHttp() throws MalformedURLException, IOException {
        URLConnection con = new URL("http://" + ServerConstants.getWwwHostNamePort()).openConnection();
        con.setRequestProperty("Origin", "https://" + ServerConstants.getWwwHostNamePortSecure());
        assertTrue( !IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

    @Test
    public void testIncorrectOriginHeader() throws MalformedURLException, IOException {
        HttpURLConnection con = (HttpURLConnection) new URL("https://" + ServerConstants.getWwwHostNamePortSecure() + "/login").openConnection();
        con.setRequestProperty("Origin", "https://evilpageandatleastnotcacert.com");
        assertTrue(IOUtils.readURL(con).contains("No cross domain access allowed."));
    }

}
