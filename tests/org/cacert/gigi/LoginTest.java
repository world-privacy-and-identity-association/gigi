package org.cacert.gigi;

import java.io.IOException;
import static org.junit.Assert.*;
import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class LoginTest extends ManagedTest {

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

}
