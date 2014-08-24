package org.cacert.gigi.ping;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.pages.account.DomainOverview;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.cacert.gigi.util.RandomToken;
import org.junit.Test;

public class TestDNS extends ManagedTest {

    @Test
    public void testDNSSanity() throws IOException {

        String token = RandomToken.generateToken(16);
        String value = RandomToken.generateToken(16);

        Process p = updateDNS(token, value);
        String reRead = new String(IOUtils.readURL(p.getInputStream()));
        reRead = reRead.trim();
        reRead = reRead.substring(1, reRead.length() - 1);
        assertEquals(value, reRead);

    }

    @Test
    public void testEmailAndDNS() throws IOException, InterruptedException, SQLException {
        String email = createUniqueName() + "@example.org";
        int uid = createVerifiedUser("a", "b", email, TEST_PASSWORD);
        String cookie = login(email, TEST_PASSWORD);

        String test = getTestProps().getProperty("domain.dnstest");
        URL u = new URL("https://" + getServerName() + DomainOverview.PATH);
        URLConnection openConnection = u.openConnection();
        openConnection.setRequestProperty("Cookie", cookie);
        String content1 = IOUtils.readURL(openConnection);
        String csrf = getCSRF(1, content1);

        Pattern p = Pattern.compile("cacert-([A-Za-z0-9]+) IN TXT ([A-Za-z0-9]+)");
        Matcher m = p.matcher(content1);
        m.find();
        updateDNS(m.group(1), m.group(2));

        String content = "newdomain=" + URLEncoder.encode(test, "UTF-8") + //
                "&emailType=y&email=2&DNSType=y" + //
                "&ssl-type-0=direct&ssl-port-0=" + //
                "&ssl-type-1=direct&ssl-port-1=" + //
                "&ssl-type-2=direct&ssl-port-2=" + //
                "&ssl-type-3=direct&ssl-port-3=" + //
                "&adddomain&csrf=" + csrf;
        openConnection = u.openConnection();
        openConnection.setRequestProperty("Cookie", cookie);
        openConnection.setDoOutput(true);
        openConnection.getOutputStream().write(content.getBytes());
        openConnection.getHeaderField("Location");

        String newcontent = IOUtils.readURL(cookie(u.openConnection(), cookie));
        Pattern dlink = Pattern.compile(DomainOverview.PATH + "([0-9]+)'>");
        Matcher m1 = dlink.matcher(newcontent);
        m1.find();
        URL u2 = new URL(u.toString() + m1.group(1));

        TestMail mail = getMailReciever().recieve();
        String link = mail.extractLink();
        new URL(link).openConnection().getHeaderField("");

        PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT COUNT(*) FROM domainPinglog");
        while (true) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) >= 2) {
                break;
            }
            Thread.sleep(200);
        }

        newcontent = IOUtils.readURL(cookie(u2.openConnection(), cookie));
        Pattern pat = Pattern.compile("<td>dns</td>\\s*<td>success</td>");
        assertTrue(newcontent, pat.matcher(newcontent).find());
        pat = Pattern.compile("<td>email</td>\\s*<td>success</td>");
        assertTrue(newcontent, pat.matcher(newcontent).find());
    }

    private Process updateDNS(String token, String value) throws IOException, MalformedURLException {
        String test = getTestProps().getProperty("domain.dnstest");
        String targetDomain = "cacert-" + token + "." + test;
        String manage = getTestProps().getProperty("domain.dnsmanage");
        String url = manage + "t1=" + token + "&t2=" + value;
        assertEquals(200, ((HttpURLConnection) new URL(url).openConnection()).getResponseCode());

        Process p = Runtime.getRuntime().exec(new String[] {
                "dig", "@" + getTestProps().getProperty("domain.testns"), "+short", "TXT", targetDomain
        });
        return p;
    }

}
