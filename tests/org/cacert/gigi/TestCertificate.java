package org.cacert.gigi;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

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
		c.issue();
		c.waitFor(60000);
		final X509Certificate ce = c.cert();
		testLogin(pk, ce, true);
	}

	private void testLogin(final PrivateKey pk, final X509Certificate ce, boolean success)
		throws NoSuchAlgorithmException, KeyManagementException, IOException, MalformedURLException {
		KeyManager km = new X509KeyManager() {

			@Override
			public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
				return "client";
			}

			@Override
			public String chooseServerAlias(String arg0, Principal[] arg1, Socket arg2) {
				return null;
			}

			@Override
			public X509Certificate[] getCertificateChain(String arg0) {
				return new X509Certificate[] { ce };
			}

			@Override
			public String[] getClientAliases(String arg0, Principal[] arg1) {
				return new String[] { "client" };
			}

			@Override
			public PrivateKey getPrivateKey(String arg0) {
				if (arg0.equals("client")) {
					return pk;
				}
				return null;
			}

			@Override
			public String[] getServerAliases(String arg0, Principal[] arg1) {
				return new String[] { "client" };
			}
		};
		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(new KeyManager[] { km }, null, null);

		HttpURLConnection connection = (HttpURLConnection) new URL("https://"
			+ getServerName().replaceFirst("^www.", "secure.") + "/login").openConnection();
		if (connection instanceof HttpsURLConnection) {
			((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
		}
		if (success) {
			assertEquals(302, connection.getResponseCode());
			assertEquals("https://" + getServerName().replaceFirst("^www.", "secure.").replaceFirst(":443$", "") + "/",
				connection.getHeaderField("Location").replaceFirst(":443$", ""));
		} else {
			assertNotEquals(302, connection.getResponseCode());
			assertNull(connection.getHeaderField("Location"));
		}
	}

	@Test
	public void testCertLifeCycle() throws IOException, GeneralSecurityException, SQLException, InterruptedException {
		String[] key1 = generateCSR("/CN=testmail@example.com");
		Certificate c = new Certificate(1, "/CN=testmail@example.com", "sha256", key1[1]);
		final PrivateKey pk = PemKey.parsePEMPrivateKey(key1[0]);

		testFails(CertificateStatus.DRAFT, c);
		c.issue();

		testFails(CertificateStatus.SIGNING, c);
		c.waitFor(60000);

		testFails(CertificateStatus.ISSUED, c);
		X509Certificate cert = c.cert();
		testLogin(pk, cert, true);
		c.revoke();

		testFails(CertificateStatus.BEING_REVOKED, c);
		c.waitFor(60000);

		testFails(CertificateStatus.REVOKED, c);
		testLogin(pk, cert, false);

	}

	private void testFails(CertificateStatus status, Certificate c) throws IOException, GeneralSecurityException,
		SQLException {
		if (status != CertificateStatus.ISSUED) {
			try {
				c.revoke();
				fail("is in invalid state");
			} catch (IllegalStateException ise) {

			}
		}
		if (status != CertificateStatus.DRAFT) {
			try {
				c.issue();
				fail("is in invalid state");
			} catch (IllegalStateException ise) {

			}
		}
		if (status != CertificateStatus.ISSUED) {
			try {
				c.cert();
				fail("is in invalid state");
			} catch (IllegalStateException ise) {

			}
		}
	}
}
