package club.wpia.gigi.ping;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Date;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Certificate.CertificateStatus;
import club.wpia.gigi.dbObjects.Certificate.SANType;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingExecution;
import club.wpia.gigi.dbObjects.DomainPingType;
import club.wpia.gigi.pages.account.domain.EditDomain;
import club.wpia.gigi.ping.DomainPinger.PingState;
import club.wpia.gigi.testUtils.PingTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.RandomToken;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;
import club.wpia.gigi.util.SimpleSigner;

public class TestTiming extends PingTest {

    @Test
    public void httpAndMailSuccessCert() throws GigiApiException, IOException, InterruptedException, GeneralSecurityException {
        httpAndMailSuccess(true, false);
    }

    @Test
    public void httpAndMailSuccessCertAndCorrect() throws GigiApiException, IOException, InterruptedException, GeneralSecurityException {
        httpAndMailSuccess(true, true);
    }

    @Test
    public void httpAndMailSuccessNoCert() throws GigiApiException, IOException, InterruptedException, GeneralSecurityException {
        httpAndMailSuccess(false, false);
    }

    public void httpAndMailSuccess(boolean certs, boolean correct) throws GigiApiException, IOException, InterruptedException, GeneralSecurityException {
        String test = getTestProps().getProperty("domain.http");
        assumeNotNull(test);

        // When we have a domain.
        Domain d = new Domain(u, u, test);
        String token = RandomToken.generateToken(16);
        String value = RandomToken.generateToken(16);

        // If we run the sub case that we have certificates on the domain,
        // create a certificate now.
        Certificate c = null;
        if (certs) {
            KeyPair kp = generateKeypair();
            String key = generatePEMCSR(kp, "CN=testmail@example.com");
            c = new Certificate(u, u, Certificate.buildDN("CN", "testmail@example.com"), Digest.SHA256, key, CSRType.CSR, CertificateProfile.getByName("server"), new Certificate.SubjectAlternateName(SANType.DNS, test));
            await(c.issue(null, "2y", u));
        }

        // Register HTTP and Email pings.
        updateService(token, value, "http");
        d.addPing(DomainPingType.EMAIL, "postmaster");
        d.addPing(DomainPingType.HTTP, token + ":" + value);

        // Two successful pings
        getMailReceiver().receive("postmaster@" + test).verify();
        waitForPings(2);

        assertEquals(0, countFailed(d.getPings(), 2));

        // An own Pinger Daemon to control ping execution locally.
        PingerDaemon pd = new PingerDaemon(null);
        pd.initializeConnectionUsage();

        // After 6 months the pings are executed again
        pd.executeNeededPings(new Date(System.currentTimeMillis() + 6 * 31 * 24 * 60 * 60L * 1000));
        getMailReceiver().receive("postmaster@" + test).verify();
        waitForPings(4);
        assertEquals(0, countFailed(d.getPings(), 4));

        // After 6 months the pings are executed again, but when the HTTP file
        // is wrong, that ping fails.
        updateService(token, value + "broken", "http");
        // Note that the time is still 6 months in the future, as the pings from
        // before were still executed (and logged)
        // as executed now.
        pd.executeNeededPings(new Date(System.currentTimeMillis() + 6 * 31 * 24 * 60 * 60L * 1000));
        getMailReceiver().receive("postmaster@" + test).verify();
        waitForPings(6);
        assertEquals(1, countFailed(d.getPings(), 6));
        // Which renders the domain invalid
        assertFalse(d.isVerified());

        if (certs) {
            // And the user gets a warning-mail if there was a cert
            TestMail mail = getMailReceiver().receive(u.getEmail());
            assertThat(mail.getMessage(), CoreMatchers.containsString(d.getSuffix()));
            assertThat(mail.getMessage(), CoreMatchers.containsString(c.getSerial()));
            if ( !correct) {
                // If the user ignores the warning, after two weeks
                pd.executeNeededPings(new Date(System.currentTimeMillis() + 15 * 24 * 60 * 60L * 1000));
                // The user receives another warning mail.
                mail = getMailReceiver().receive(u.getEmail());
                assertThat(mail.getMessage(), CoreMatchers.containsString("https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + EditDomain.PATH + d.getId()));
                assertThat(mail.getMessage(), CoreMatchers.containsString(d.getSuffix()));
                assertThat(mail.getMessage(), CoreMatchers.containsString(c.getSerial()));
                // And when the revocation is carried out
                SimpleSigner.ping();
                // ... and the certificate gets revoked.
                assertEquals(CertificateStatus.REVOKED, c.getStatus());
            } else {
                // But if the user corrects the ping, ...
                updateService(token, value, "http");
                // ... and the ping is re-executed,
                pd.handle(getPing(d.getConfiguredPings(), DomainPingType.HTTP));
                waitForPings(7);
                assertEquals(1, countFailed(d.getPings(), 7));

                // Even after two weeks
                pd.executeNeededPings(new Date(System.currentTimeMillis() + 15 * 24 * 60 * 60L * 1000));
                // and all resulting jobs are executed
                SimpleSigner.ping();
                // ... the certificate stays valid.
                assertEquals(CertificateStatus.ISSUED, c.getStatus());
            }
        } else {
            // otherwise there is no mail
        }

    }

    private DomainPingConfiguration getPing(List<DomainPingConfiguration> cp, DomainPingType tp) {
        for (DomainPingConfiguration d : cp) {
            if (d.getType() == tp) {
                return d;
            }
        }
        throw new Error("Type not found.");
    }

    private int countFailed(DomainPingExecution[] pg, int count) {
        assertEquals(count, pg.length);
        int fld = 0;
        for (DomainPingExecution e : pg) {
            PingState state = e.getState();
            if (e.getConfig().getType() == DomainPingType.HTTP) {
                if (state == PingState.FAILED) {
                    fld++;
                    continue;
                }
            }
            assertEquals(PingState.SUCCESS, state);
        }
        return fld;
    }

}
