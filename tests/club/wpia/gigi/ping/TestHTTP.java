package club.wpia.gigi.ping;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingType;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.PingTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.RandomToken;
import club.wpia.gigi.util.SystemKeywords;

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

        Matcher m = initailizeDomainForm();
        updateService(m.group(1) + (httpVariant == 1 ? "a" : ""), m.group(2) + (httpVariant == 2 ? "a" : ""), "http");

        String content = "newdomain=" + URLEncoder.encode(test, "UTF-8") + //
                "&emailType=y&email=2&HTTPType=y" + //
                "&ssl-type-0=direct&ssl-port-0=" + //
                "&ssl-type-1=direct&ssl-port-1=" + //
                "&ssl-type-2=direct&ssl-port-2=" + //
                "&ssl-type-3=direct&ssl-port-3=" + //
                "&adddomain&csrf=" + csrf;
        String p2 = sendDomainForm(content);

        TestMail mail = getMailReceiver().receive("postmaster@" + test);
        if (emailVariant == 0) {
            mail.verify();
        }
        waitForPings(2);

        String newcontent = IOUtils.readURL(get(p2));
        Pattern pat = Pattern.compile("<td>http</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successHTTP ^ pat.matcher(newcontent).find());
        pat = Pattern.compile("<td>email</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successMail ^ pat.matcher(newcontent).find());

        if (successHTTP) { // give it a second try
            int id = Integer.parseInt(p2.replaceFirst("^.*/([0-9]+)$", "$1"));
            Domain d = Domain.getById(id);
            DomainPingConfiguration dpc = null;
            for (DomainPingConfiguration conf : d.getConfiguredPings()) {
                if (conf.getType() == DomainPingType.HTTP) {
                    dpc = conf;
                    break;
                }
            }
            if (dpc == null) {
                fail("Http config not found");
                return;
            }
            String res = executeBasicWebInteraction(cookie, p2, "configId=" + dpc.getId());
            assertThat(res, containsString("only allowed after"));
        }
    }

    private String readHTTP(String token) throws IOException {
        String httpDom = getTestProps().getProperty("domain.http");
        assumeNotNull(httpDom);
        URL u = new URL("http://" + httpDom + "/" + SystemKeywords.HTTP_CHALLENGE_PREFIX + token + ".txt");
        return IOUtils.readURL(new InputStreamReader(u.openStream(), "UTF-8")).trim();

    }

    @Test
    public void testHttpRedirect() throws IOException, SQLException, InterruptedException {
        try (ServerSocket s = openServer()) {
            testHttpRedirect(s, true);
        }
    }

    @Test
    public void testHttpNoRedirect() throws IOException, SQLException, InterruptedException {
        try (ServerSocket s = openServer()) {
            testHttpRedirect(s, false);
        }
    }

    private ServerSocket openServer() {
        String localHTTP = getTestProps().getProperty("domain.localHTTP");
        assumeNotNull(localHTTP);
        try {
            return new ServerSocket(Integer.parseInt(localHTTP));
        } catch (IOException e) {
            throw new Error("Requires a free port " + localHTTP);
        }
    }

    public void testHttpRedirect(ServerSocket s, boolean doRedirect) throws IOException, SQLException, InterruptedException {
        String test = getTestProps().getProperty("domain.local");
        assumeNotNull(test);

        Matcher m = initailizeDomainForm();

        String content = "newdomain=" + URLEncoder.encode(test, "UTF-8") + //
                "&emailType=y&email=2&HTTPType=y" + //
                "&ssl-type-0=direct&ssl-port-0=" + //
                "&ssl-type-1=direct&ssl-port-1=" + //
                "&ssl-type-2=direct&ssl-port-2=" + //
                "&ssl-type-3=direct&ssl-port-3=" + //
                "&adddomain&csrf=" + csrf;
        String p2 = sendDomainForm(content);
        try (Socket s0 = s.accept()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(s0.getInputStream(), "UTF-8"));
            String fst = br.readLine();
            assertEquals("GET /" + SystemKeywords.HTTP_CHALLENGE_PREFIX + m.group(1) + ".txt HTTP/1.1", fst);
            while ( !"".equals(br.readLine())) {
            }
            String res = m.group(2);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s0.getOutputStream(), "UTF-8"));
            if ( !doRedirect) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-length: " + res.length());
                out.println();
                out.print(res);
            } else {
                out.println("HTTP/1.1 302 Moved");
                out.println("Location: /token");
                out.println();
            }
            out.flush();
        }
        waitForPings(2);

        TestMail mail = getMailReceiver().receive("postmaster@" + test);
        mail.verify();

        String newcontent = IOUtils.readURL(get(p2));
        Pattern pat = Pattern.compile("<td>http</td>\\s*<td>success</td>");
        pat = Pattern.compile("<td>http</td>\\s*<td>([^<]*)</td>\\s*<td>([^<]*)</td>\\s*<td>([^<]*)</td>");
        Matcher m0 = pat.matcher(newcontent);
        assertTrue(newcontent, m0.find());
        if (doRedirect) {
            assertEquals("failed", m0.group(1));
            assertThat(m0.group(3), containsString("code 302"));
        } else {
            assertEquals("success", m0.group(1));
        }
        pat = Pattern.compile("<td>email</td>\\s*<td>success</td>");
        assertTrue(newcontent, pat.matcher(newcontent).find());

    }
}
