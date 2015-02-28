package org.cacert.gigi.ping;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.dbObjects.DomainPingConfiguration.PingType;
import org.cacert.gigi.pages.account.domain.DomainOverview;
import org.cacert.gigi.testUtils.IOUtils;
import org.cacert.gigi.testUtils.PingTest;
import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;
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
    public void httpAndMailSuccess() throws Exception {
        testEmailAndHTTP(0, 0, true, true);
    }

    @Test
    public void httpFailKeyAndMailSuccess() throws Exception {
        testEmailAndHTTP(1, 0, false, true);
    }

    @Test
    public void httpFailValAndMailFail() throws Exception {
        testEmailAndHTTP(2, 1, false, false);
    }

    public void testEmailAndHTTP(int httpVariant, int emailVariant, boolean successHTTP, boolean successMail) throws IOException, InterruptedException, SQLException, GigiApiException {

        String test = getTestProps().getProperty("domain.http");
        assumeNotNull(test);

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

        TestMail mail = getMailReciever().receive();
        if (emailVariant == 0) {
            mail.verify();
        }
        waitForPings(2);

        String newcontent = IOUtils.readURL(cookie(u2.openConnection(), cookie));
        Pattern pat = Pattern.compile("<td>http</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successHTTP ^ pat.matcher(newcontent).find());
        pat = Pattern.compile("<td>email</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successMail ^ pat.matcher(newcontent).find());

        if (successHTTP) { // give it a second try
            int id = Integer.parseInt(u2.toString().replaceFirst("^.*/([0-9]+)$", "$1"));
            Domain d = Domain.getById(id);
            DomainPingConfiguration dpc = null;
            for (DomainPingConfiguration conf : d.getConfiguredPings()) {
                if (conf.getType() == PingType.HTTP) {
                    dpc = conf;
                    break;
                }
            }
            if (dpc == null) {
                fail("Http config not found");
            }
            String res = executeBasicWebInteraction(cookie, u2.getPath(), "configId=" + dpc.getId());
            assertThat(res, containsString("only allowed after"));
        }
    }

    private String readHTTP(String token) throws IOException {
        String httpDom = getTestProps().getProperty("domain.http");
        assumeNotNull(httpDom);
        URL u = new URL("http://" + httpDom + "/cacert-" + token + ".txt");
        return IOUtils.readURL(new InputStreamReader(u.openStream(), "UTF-8")).trim();

    }
}
