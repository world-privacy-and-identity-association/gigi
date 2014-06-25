package org.cacert.gigi;

import java.io.IOException;
import static org.junit.Assert.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class LoginTest extends ManagedTest {
	public static final String secureReference = "/account/certs/email";
	@Test
	public void testLoginUnverified() throws IOException {
		long uniq = System.currentTimeMillis();
		String email = "system" + uniq + "@testmail.org";
		String pw = "1'aAaA";
		registerUser("an", "bn", email, pw);
		waitForMail();
		assertFalse(isLoggedin(login(email, pw)));
	}
	@Test
	public void testLoginVerified() throws IOException {
		long uniq = System.currentTimeMillis();
		String email = "system2" + uniq + "@testmail.org";
		String pw = "1'aAaA";
		createVerifiedUser("an", "bn", email, pw);
		assertTrue(isLoggedin(login(email, pw)));
	}
	public boolean isLoggedin(String cookie) throws IOException {
		URL u = new URL("https://" + getServerName() + secureReference);
		HttpURLConnection huc = (HttpURLConnection) u.openConnection();
		huc.addRequestProperty("Cookie", cookie);
		return huc.getResponseCode() == 200;
	}
	public String login(String email, String pw) throws IOException {
		URL u = new URL("https://" + getServerName() + "/login");
		HttpURLConnection huc = (HttpURLConnection) u.openConnection();
		huc.setDoOutput(true);
		OutputStream os = huc.getOutputStream();
		String data = "username=" + URLEncoder.encode(email, "UTF-8")
				+ "&password=" + URLEncoder.encode(pw, "UTF-8");
		os.write(data.getBytes());
		os.flush();
		String headerField = huc.getHeaderField("Set-Cookie");
		headerField = headerField.substring(0, headerField.indexOf(';'));
		return headerField;
	}

}
