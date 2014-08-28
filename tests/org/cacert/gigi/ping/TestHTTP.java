package org.cacert.gigi.ping;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.cacert.gigi.pages.account.DomainOverview;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.PingTest;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.cacert.gigi.util.RandomToken;
import org.junit.Test;

public class TestHTTP extends PingTest {

    @Test
    public void httpSanity() throws IOException, NamingException {

        String token = RandomToken.generateToken(16);
        String value = RandomToken.generateToken(16);

        TestDNS.updateService(token, value, "http");
        assertEquals(value, readHTTP(token));

    }

    @Test
    public void httpAndMailSuccess() throws IOException, InterruptedException, SQLException {
        testEmailAndHTTP(0, 0, true, true);
    }

    @Test
    public void httpFailKeyAndMailSuccess() throws IOException, InterruptedException, SQLException {
        testEmailAndHTTP(1, 0, false, true);
    }

    @Test
    public void httpFailValAndMailFail() throws IOException, InterruptedException, SQLException {
        testEmailAndHTTP(2, 1, false, false);
    }

    public void testEmailAndHTTP(int httpVariant, int emailVariant, boolean successHTTP, boolean successMail) throws IOException, InterruptedException, SQLException {

        String test = getTestProps().getProperty("domain.http");

        URL u = new URL("https://" + getServerName() + DomainOverview.PATH);
        Matcher m = initailizeDomainForm(u);
        updateService(m.group(1) + (httpVariant == 1 ? "a" : ""), m.group(2) + (httpVariant == 2 ? "a" : ""), "http");

        String content = "newdomain=" + URLEncoder.encode(test, "UTF-8") + //
                "&emailType=y&email=2&HTTPType=y" + //
                "&ssl-type-0=direct&ssl-port-0=" + //
                "&ssl-type-1=direct&ssl-port-1=" + //
                "&ssl-type-2=direct&ssl-port-2=" + //
                "&ssl-type-3=direct&ssl-port-3=" + //
                "&adddomain&csrf=" + csrf;
        URL u2 = sendDomainForm(u, content);

        TestMail mail = getMailReciever().recieve();
        if (emailVariant == 0) {
            String link = mail.extractLink();
            new URL(link).openConnection().getHeaderField("");
        }
        waitForPings(2);

        String newcontent = IOUtils.readURL(cookie(u2.openConnection(), cookie));
        Pattern pat = Pattern.compile("<td>http</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successHTTP ^ pat.matcher(newcontent).find());
        pat = Pattern.compile("<td>email</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successMail ^ pat.matcher(newcontent).find());
    }

    private String readHTTP(String token) throws IOException {
        URL u = new URL("http://" + getTestProps().getProperty("domain.http") + "/cacert-" + token + ".txt");
        return IOUtils.readURL(new InputStreamReader(u.openStream())).trim();

    }
}
