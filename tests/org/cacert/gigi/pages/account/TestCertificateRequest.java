package org.cacert.gigi.pages.account;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.pages.account.certs.CertificateRequest;
import org.cacert.gigi.testUtils.ClientTest;
import org.junit.Test;

public class TestCertificateRequest extends ClientTest {

    KeyPair kp = generateKeypair();

    public TestCertificateRequest() throws GeneralSecurityException {}

    @Test
    public void testIssuingOtherName() throws Exception {
        try {
            new CertificateRequest(u, generatePEMCSR(kp, "CN=hansi")).draft();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("does not match the details"));
        }
    }

    @Test
    public void testIssuingDefault() throws Exception {
        new CertificateRequest(u, generatePEMCSR(kp, "CN=" + CertificateRequest.DEFAULT_CN)).draft();
    }

    @Test
    public void testIssuingRealName() throws Exception {
        new CertificateRequest(u, generatePEMCSR(kp, "CN=a b")).draft();
    }

    @Test
    public void testIssuingModifiedName() throws Exception {
        try {
            new CertificateRequest(u, generatePEMCSR(kp, "CN=a ab")).draft();
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("does not match the details"));
        }

    }
}