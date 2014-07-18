package org.cacert.gigi;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import org.cacert.gigi.Certificate.CertificateStatus;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.testUtils.PemKey;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCertificate extends ManagedTest {
	@Test
	public void testClientCertLoginStates() throws IOException, GeneralSecurityException, SQLException,
		InterruptedException {
		String[] key1 = generateCSR("/CN=testmail@example.com");
		Certificate c = new Certificate(1, "/CN=testmail@example.com", "sha256", key1[1]);
		final PrivateKey pk = PemKey.parsePEMPrivateKey(key1[0]);
		c.issue().waitFor(60000);
		final X509Certificate ce = c.cert();
		assertNotNull(login(pk, ce));
	}

	@Test
	public void testCertLifeCycle() throws IOException, GeneralSecurityException, SQLException, InterruptedException {
		String[] key1 = generateCSR("/CN=testmail@example.com");
		Certificate c = new Certificate(1, "/CN=testmail@example.com", "sha256", key1[1]);
		final PrivateKey pk = PemKey.parsePEMPrivateKey(key1[0]);

		testFails(CertificateStatus.DRAFT, c);
		c.issue().waitFor(60000);

		testFails(CertificateStatus.ISSUED, c);
		X509Certificate cert = c.cert();
		assertNotNull(login(pk, cert));
		c.revoke().waitFor(60000);

		testFails(CertificateStatus.REVOKED, c);
		assertNull(login(pk, cert));

	}

	private void testFails(CertificateStatus status, Certificate c) throws IOException, GeneralSecurityException,
		SQLException {
		assertEquals(status, c.getStatus());
		if (status != CertificateStatus.ISSUED) {
			try {
				c.revoke();
				fail(status + " is in invalid state");
			} catch (IllegalStateException ise) {

			}
		}
		if (status != CertificateStatus.DRAFT) {
			try {
				c.issue();
				fail(status + " is in invalid state");
			} catch (IllegalStateException ise) {

			}
		}
		if (status != CertificateStatus.ISSUED) {
			try {
				c.cert();
				fail(status + " is in invalid state");
			} catch (IllegalStateException ise) {

			}
		}
	}
}
