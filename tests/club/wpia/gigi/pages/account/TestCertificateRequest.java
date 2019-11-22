package club.wpia.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Locale;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.TimeConditions;

public class TestCertificateRequest extends ClientTest {

    KeyPair kp = generateKeypair();

    AuthorizationContext ac;

    public TestCertificateRequest() throws GeneralSecurityException, IOException, GigiApiException {
        ac = new AuthorizationContext(u, u, false);
        makeAgent(u.getId());
    }

    @Test
    public void testIssuingOtherName() throws Exception {
        try {
            new CertificateRequest(ac, generatePEMCSR(kp, "CN=hansi")).draft();
            fail();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("name you entered was invalid"));
        }
    }

    @Test
    public void testIssuingDefault() throws Exception {
        new CertificateRequest(ac, generatePEMCSR(kp, "CN=" + CertificateRequest.DEFAULT_CN + ",EMAIL=" + email)).draft();
    }

    @Test
    public void testIssuingRealName() throws Exception {
        new CertificateRequest(ac, generatePEMCSR(kp, "CN=a b,EMAIL=" + email)).draft();
    }

    @Test
    public void testIssuingModifiedName() throws Exception {
        try {
            new CertificateRequest(ac, generatePEMCSR(kp, "CN=a ab")).draft();
            fail();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("name you entered was invalid"));
        }

    }

    // TODO annotate that this depends on default config
    @Test
    public void testCodesignModifiedName() throws Exception {
        try {
            u.grantGroup(getSupporter(), Group.CODESIGNING);
            CertificateRequest cr = new CertificateRequest(ac, generatePEMCSR(kp, "CN=a ab"));
            cr.update("name", "SHA512", "code-a", null, null, "email:" + email);
            cr.draft();
            fail();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("does not match the details"));
        }

    }

    // TODO annotate that this depends on default config
    @Test
    public void testCodesignNoPermModifiedName() throws Exception {
        try {
            CertificateRequest cr = new CertificateRequest(ac, generatePEMCSR(kp, "CN=a ab"));
            cr.update("name", "SHA512", "code-a", null, null, "email:" + email);
            cr.draft();
            fail();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("Certificate Profile is invalid."));
        }

    }

    @Test
    public void testPingPeriodOneAddress() throws IOException, GeneralSecurityException, GigiApiException {
        // get new email address with last ping in past
        String furtherEmail = createUniqueName() + "@example.org";
        new EmailAddress(u, furtherEmail, Locale.ENGLISH);
        getMailReceiver().receive(furtherEmail);
        try (GigiPreparedStatement stmt = new GigiPreparedStatement("UPDATE `emailPinglog` SET `status`='success'::`pingState`, `when` = (now() - interval '1 months' * ?::INTEGER) WHERE `email`=? ")) {
            stmt.setInt(1, TimeConditions.getInstance().getEmailPingMonths());
            stmt.setString(2, furtherEmail);
            stmt.executeUpdate();
        }

        try {
            CertificateRequest cr = new CertificateRequest(ac, generatePEMCSR(kp, "CN=a ab"));
            cr.update("name", "SHA512", "mail", null, null, "email:" + furtherEmail);
            cr.draft();
            fail();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("needs a verification via email ping within the past"));
        }

    }

    @Test
    public void testPingPeriodTwoAddresses() throws IOException, GeneralSecurityException, GigiApiException {
        // get new email address with last ping in past
        String furtherEmail = createUniqueName() + "@example.org";
        new EmailAddress(u, furtherEmail, Locale.ENGLISH);
        getMailReceiver().receive(furtherEmail);
        try (GigiPreparedStatement stmt = new GigiPreparedStatement("UPDATE `emailPinglog` SET `status`='success'::`pingState`, `when` = (now() - interval '1 months' * ?::INTEGER) WHERE `email`=? ")) {
            stmt.setInt(1, TimeConditions.getInstance().getEmailPingMonths());
            stmt.setString(2, furtherEmail);
            stmt.executeUpdate();
        }

        try {
            CertificateRequest cr = new CertificateRequest(ac, generatePEMCSR(kp, "CN=a ab"));
            cr.update("name", "SHA512", "mail", null, null, "email:" + furtherEmail + ",email:" + email);
            cr.draft();
            fail();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("needs a verification via email ping within the past"));
        }

    }
}
