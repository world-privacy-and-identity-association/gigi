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
		registerUser("an", "bn", email, TEST_PASSWORD);
		waitForMail();
		assertFalse(isLoggedin(login(email, TEST_PASSWORD)));
	}

	@Test
	public void testLoginVerified() throws IOException {
		long uniq = System.currentTimeMillis();
		String email = "system2" + uniq + "@testmail.org";
		createVerifiedUser("an", "bn", email, TEST_PASSWORD);
		assertTrue(isLoggedin(login(email, TEST_PASSWORD)));
	}

}
