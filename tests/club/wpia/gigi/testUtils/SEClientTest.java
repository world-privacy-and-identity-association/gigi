package club.wpia.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.admin.support.SupportEnterTicketPage;

/**
 * Superclass for testsuites in a scenario where there is a supporter, who is
 * already logged on.
 */
public abstract class SEClientTest extends ClientTest {

    public SEClientTest() throws IOException, GigiApiException {
        grant(u, Group.SUPPORTER);
        try {
            KeyPair kp = generateKeypair();
            String csr = generatePEMCSR(kp, "CN=" + u.getPreferredName().toString());
            Certificate c = new Certificate(u, u, Certificate.buildDN("CN", u.getPreferredName().toString()), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
            final PrivateKey pk = kp.getPrivate();
            await(c.issue(null, "2y", u));
            final X509Certificate ce = c.cert();
            c.setLoginEnabled(true);
            loginCertificate = c;
            loginPrivateKey = pk;
            cookie = login(pk, ce);
        } catch (InterruptedException e) {
            throw new GigiApiException(e.toString());
        } catch (GeneralSecurityException e) {
            throw new GigiApiException(e.toString());
        }
        assertEquals(302, post(cookie, SupportEnterTicketPage.PATH, "ticketno=a20140808.8&setTicket=action", 0).getResponseCode());
    }

}
