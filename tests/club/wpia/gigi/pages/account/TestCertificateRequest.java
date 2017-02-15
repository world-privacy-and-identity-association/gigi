package club.wpia.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.testUtils.ClientTest;
import club.wpia.gigi.util.AuthorizationContext;

public class TestCertificateRequest extends ClientTest {

    KeyPair kp = generateKeypair();

    AuthorizationContext ac;

    public TestCertificateRequest() throws GeneralSecurityException, IOException, GigiApiException {
        ac = new AuthorizationContext(u, u);
        makeAssurer(u.getId());
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
}
