package club.wpia.gigi.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.testUtils.ClientTest;

@RunWith(Parameterized.class)
public class TestCAAValidation extends ClientTest {

    @Parameters(name = "CAATest({0}) = {1}")
    public static Iterable<Object[]> genParams() throws IOException {
        initEnvironment();

        String caa = (String) getTestProps().get("domain.CAAtest");
        assumeNotNull(caa);
        String[] parts = caa.split(" ");
        Object[][] res = new Object[parts.length][];
        for (int i = 0; i < res.length; i++) {
            char firstChar = parts[i].charAt(0);
            if (firstChar != '-' && firstChar != '+') {
                throw new Error("malformed CAA test vector");
            }
            res[i] = new Object[] {
                    parts[i].substring(1), firstChar == '+'
            };
        }
        return Arrays.<Object[]>asList(res);
    }

    @Parameter(0)
    public String domain;

    @Parameter(1)
    public Boolean success;

    @Test
    public void testCAA() throws GigiApiException {
        assertEquals(success, CAA.verifyDomainAccess(u, CertificateProfile.getByName("server"), domain));
    }

    @Test
    public void testCAACert() throws GeneralSecurityException, IOException, GigiApiException, InterruptedException {
        Domain d = new Domain(u, u, PublicSuffixes.getInstance().getRegistrablePart(domain));
        verify(d);
        String csr = generatePEMCSR(generateKeypair(), "CN=test");
        CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u, false), csr);
        try {
            cr.update("", Digest.SHA512.toString(), "server", null, null, "dns:" + domain + "\n");
        } catch (GigiApiException e) {
            assertThat(e.getMessage(), containsString("has been removed"));
            assertFalse(success);
            return;
        }
        assertTrue(success);
        Certificate draft = cr.draft();
        Job j = draft.issue(null, "2y", u);
        await(j);

        assertEquals(CertificateStatus.ISSUED, draft.getStatus());
    }

}
