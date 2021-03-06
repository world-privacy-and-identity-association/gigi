package club.wpia.gigi.ping;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.junit.Test;

import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.PingTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.DNSUtil;
import club.wpia.gigi.util.RandomToken;
import club.wpia.gigi.util.SystemKeywords;

public class TestDNS extends PingTest {

    @Test
    public void dnsSanity() throws IOException, NamingException {

        String token = RandomToken.generateToken(16);
        String value = RandomToken.generateToken(16);

        updateService(token, value, "dns");
        assertEquals(value, readDNS(token));

    }

    @Test
    public void emailAndDNSSuccess() throws IOException, InterruptedException, SQLException, NamingException {
        testEmailAndDNS(0, 0, true, true);
    }

    @Test
    public void dnsFail() throws IOException, InterruptedException, SQLException, NamingException {
        testEmailAndDNS(1, 0, false, true);
    }

    @Test
    public void dnsContentFail() throws IOException, InterruptedException, SQLException, NamingException {
        testEmailAndDNS(2, 0, false, true);
    }

    @Test
    public void emailFail() throws IOException, InterruptedException, SQLException, NamingException {
        testEmailAndDNS(0, 1, true, false);
    }

    @Test
    public void emailAndDNSFail() throws IOException, InterruptedException, SQLException, NamingException {
        testEmailAndDNS(2, 1, false, false);
    }

    public void testEmailAndDNS(int dnsVariant, int emailVariant, boolean successDNS, boolean successMail) throws IOException, InterruptedException, SQLException, NamingException {

        String test = getTestProps().getProperty("domain.dnstest");
        assumeNotNull(test);

        Matcher m = initailizeDomainForm();
        updateService(m.group(1) + (dnsVariant == 1 ? "a" : ""), m.group(2) + (dnsVariant == 2 ? "a" : ""), "dns");

        String content = "newdomain=" + URLEncoder.encode(test, "UTF-8") + //
                "&emailType=y&email=2&DNSType=y" + //
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
        Pattern pat = Pattern.compile("<td>dns</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successDNS ^ pat.matcher(newcontent).find());
        pat = Pattern.compile("<td>email</td>\\s*<td>success</td>");
        assertTrue(newcontent, !successMail ^ pat.matcher(newcontent).find());
    }

    private String readDNS(String token) throws NamingException {
        String test = getTestProps().getProperty("domain.dnstest");
        assumeNotNull(test);
        String targetDomain = token + "." + SystemKeywords.DNS_PREFIX + "._auth." + test;
        String testns = getTestProps().getProperty("domain.testns");
        assumeNotNull(testns);
        String[] data = DNSUtil.getTXTEntries(targetDomain, testns);
        assertEquals(1, data.length);
        return data[0];

    }
}
