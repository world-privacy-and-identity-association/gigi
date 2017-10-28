package club.wpia.gigi.pages.admin;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.account.certs.Certificates;
import club.wpia.gigi.pages.admin.support.FindCertPage;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.testUtils.IOUtils;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class TestSEAdminPageCertSearch extends ClientTest {

    private Certificate c;

    private String certMail;

    public TestSEAdminPageCertSearch() throws IOException, GigiApiException, GeneralSecurityException, InterruptedException {
        grant(u, Group.SUPPORTER);
        cookie = login(email, TEST_PASSWORD);
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());

        certMail = uniq + "_certowner@example.com";
        int id = createVerifiedUser("fn", "ln", certMail, TEST_PASSWORD);
        User u1 = User.getById(id);
        KeyPair kp = generateKeypair();
        String key = generatePEMCSR(kp, "CN=" + certMail);
        c = new Certificate(u1, u1, Certificate.buildDN("CN", certMail), Digest.SHA512, key, CSRType.CSR, getClientProfile(), new Certificate.SubjectAlternateName(SANType.EMAIL, certMail));
        await(c.issue(null, "2y", u));
    }

    @Test
    public void testSerialSearch() throws IOException {
        URLConnection uc = post(cookie, FindCertPage.PATH, "certType=serial&process=Next&cert=" + c.getSerial(), 0);
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + Certificates.SUPPORT_PATH + "/" + c.getSerial(), uc.getHeaderField("Location"));
    }

    @Test
    public void testEmailSearch() throws IOException {
        URLConnection uc = post(cookie, FindCertPage.PATH, "certType=email&process=Next&cert=" + URLEncoder.encode(certMail, "UTF-8"), 0);
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + Certificates.SUPPORT_PATH + "/" + c.getSerial(), uc.getHeaderField("Location"));
    }

    @Test
    public void testDetails() throws IOException {
        String s = IOUtils.readURL(get(Certificates.SUPPORT_PATH + "/" + c.getSerial()));
        assertThat(s, CoreMatchers.containsString("SHA512"));
        assertThat(s, CoreMatchers.containsString(certMail));
        assertThat(s, CoreMatchers.containsString(c.getSerial()));
        assertThat(s, CoreMatchers.containsString("ISSUED"));
    }

    @Test
    public void testRevoke() throws IOException {
        URLConnection conn = post(Certificates.SUPPORT_PATH + "/" + c.getSerial(), "action=revoke");
        assertEquals("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + Certificates.SUPPORT_PATH + "/" + c.getSerial(), conn.getHeaderField("Location"));
        for (int i = 0; i < 2; i++) {
            TestMail tm = getMailReceiver().receive(i == 0 ? ServerConstants.getSupportMailAddress() : certMail);
            assertThat(tm.getMessage(), CoreMatchers.containsString(certMail));
            assertThat(tm.getMessage(), CoreMatchers.containsString(c.getSerial()));
        }
        assertEquals(CertificateStatus.REVOKED, c.getStatus());
    }

}
