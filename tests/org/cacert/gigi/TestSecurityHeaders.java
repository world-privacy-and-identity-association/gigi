package org.cacert.gigi;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestSecurityHeaders extends ManagedTest {
	@Test
	public void testSTS() throws IOException {
		HttpURLConnection uc = (HttpURLConnection) new URL("https://"
				+ getServerName()).openConnection();
		assertNotNull(uc.getHeaderField("Strict-Transport-Security"));
	}

	public void testCSP() throws IOException {
		HttpURLConnection uc = (HttpURLConnection) new URL("https://"
				+ getServerName()).openConnection();
		assertNotNull(uc.getHeaderField("Content-Security-Policy"));
	}
	public void testAllowOrigin() throws IOException {
		HttpURLConnection uc = (HttpURLConnection) new URL("https://"
				+ getServerName()).openConnection();
		assertNotNull(uc.getHeaderField("Access-Control-Allow-Origin"));

	}
}
