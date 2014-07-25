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
		EmailAddress adrr = new EmailAddress(createUniqueName() + "test@test.tld", u);
		adrr.insert(Language.getInstance("en"));
		TestMail testMail = getMailReciever().recieve();
		assertTrue(adrr.getAddress().equals(testMail.getTo()));
		String hash = testMail.extractLink().substring(testMail.extractLink().lastIndexOf('=') + 1);
		adrr.verify(hash);
		getMailReciever().clearMails();
	}

	@Test
	public void testMailAddInternalFaulty() {
		try {
			new EmailAddress("kurti ", u);
			fail();
		} catch (IllegalArgumentException e) {
			// Intended.
		}
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

	@Test
	public void testMailAddWebFaulty() throws MalformedURLException, UnsupportedEncodingException, IOException {
		String newMail = createUniqueName() + "uniexample.org";
		assertNotNull(executeBasicWebInteraction(cookie, path,
			"addmail&newemail=" + URLEncoder.encode(newMail, "UTF-8"), 1));
		EmailAddress[] addrs = u.getEmails();
		for (int i = 0; i < addrs.length; i++) {
			if (addrs[i].getAddress().equals(newMail)) {
				fail();
			}
		}
	}

	@Test
	public void testMailSetDefaultWeb() throws MalformedURLException, UnsupportedEncodingException, IOException,
		InterruptedException, GigiApiException {
		EmailAddress adrr = new EmailAddress(createUniqueName() + "test@test.tld", u);
		adrr.insert(Language.getInstance("en"));
		TestMail testMail = getMailReciever().recieve();
		assertTrue(adrr.getAddress().equals(testMail.getTo()));
		String hash = testMail.extractLink().substring(testMail.extractLink().lastIndexOf('=') + 1);
		adrr.verify(hash);
		assertNull(executeBasicWebInteraction(cookie, path, "makedefault&emailid=" + adrr.getId()));
		assertEquals(User.getById(u.getId()).getEmail(), adrr.getAddress());
		getMailReciever().clearMails();
	}

	@Test
	public void testMailSetDefaultWebUnverified() throws MalformedURLException, UnsupportedEncodingException,
		IOException, InterruptedException, GigiApiException {
		EmailAddress adrr = new EmailAddress(createUniqueName() + "test@test.tld", u);
		adrr.insert(Language.getInstance("en"));
		assertNotNull(executeBasicWebInteraction(cookie, path, "makedefault&emailid=" + adrr.getId()));
		assertNotEquals(User.getById(u.getId()).getEmail(), adrr.getAddress());
		getMailReciever().clearMails();
	}

	@Test
	public void testMailSetDefaultWebInvalidID() throws MalformedURLException, UnsupportedEncodingException,
		IOException, InterruptedException, GigiApiException {
		User u2 = User.getById(createVerifiedUser("fn", "ln", createUniqueName() + "uni@example.org", TEST_PASSWORD));
		int id = -1;
		EmailAddress[] emails = u2.getEmails();
		for (int i = 0; i < emails.length; i++) {
			if (emails[i].getAddress().equals(u2.getEmail())) {
				id = emails[i].getId();
			}
		}
		assertNotEquals(id, -1);
		assertNotNull(executeBasicWebInteraction(cookie, path, "makedefault&emailid=" + id));
		assertNotEquals(User.getById(u.getId()).getEmail(), u2.getEmail());
		getMailReciever().clearMails();
	}
}
