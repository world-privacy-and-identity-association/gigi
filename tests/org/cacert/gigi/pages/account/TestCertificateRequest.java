package org.cacert.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.account.certs.CertificateRequest;
import org.cacert.gigi.testUtils.ClientTest;
import org.cacert.gigi.util.AuthorizationContext;
import org.junit.Test;

public class TestCertificateRequest extends ClientTest {

    KeyPair kp = generateKeypair();

    AuthorizationContext ac;

    public TestCertificateRequest() throws GeneralSecurityException, IOException {
        ac = new AuthorizationContext(u, u);
        makeAssurer(u.getId());
        grant(email, Group.CODESIGNING);

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
            CertificateRequest cr = new CertificateRequest(ac, generatePEMCSR(kp, "CN=a ab"));
            System.out.println("eml");
            for (EmailAddress e : u.getEmails()) {
                System.out.println(e.getAddress());
            }
            cr.update("name", "SHA512", "code-a", null, null, "email:" + email, null, null);
            cr.draft();
            fail();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("does not match the details"));
        }

    }
}
