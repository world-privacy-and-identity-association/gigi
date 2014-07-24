package org.cacert.gigi.pages.account;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import org.cacert.gigi.EmailAddress;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.junit.Test;

public class TestMailManagement extends ManagedTest {
	private User u = User
		.getById(createVerifiedUser("fn", "ln", createUniqueName() + "uni@example.org", TEST_PASSWORD));
	private String cookie;
	private String path = MailOverview.DEFAULT_PATH;

	public TestMailManagement() throws IOException {
		cookie = login(u.getEmail(), TEST_PASSWORD);
		assertTrue(isLoggedin(cookie));
	}

	@Test
	public void testMailAddInternal() throws InterruptedException, GigiApiException {
		EmailAddress adrr = new EmailAddress("test@test.tld", u);
		adrr.insert(Language.getInstance("en"));
		TestMail testMail = getMailReciever().recieve();
		assertTrue(adrr.getAddress().equals(testMail.getTo()));
		String hash = testMail.extractLink().substring(testMail.extractLink().lastIndexOf('=') + 1);
		adrr.verify(hash);
		try {
			new EmailAddress("kurti ", u);
		} catch (IllegalArgumentException e) {
			// Intended.
			return;
		}
		fail();
	}

	@Test
	public void testMailAddWeb() throws MalformedURLException, UnsupportedEncodingException, IOException {
		String newMail = createUniqueName() + "uni@example.org";
		assertNull(executeBasicWebInteraction(cookie, path, "addmail&newemail=" + URLEncoder.encode(newMail, "UTF-8"),
			1));
		EmailAddress[] addrs = u.getEmails();
		for (int i = 0; i < addrs.length; i++) {
			if (addrs[i].getAddress().equals(newMail)) {
				return;
			}
		}
		fail();
	}

}
