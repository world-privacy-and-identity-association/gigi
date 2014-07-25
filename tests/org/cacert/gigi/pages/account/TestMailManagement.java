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
		createVerifiedEmail(u);
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
		EmailAddress adrr = createVerifiedEmail(u);
		assertNull(executeBasicWebInteraction(cookie, path, "makedefault&emailid=" + adrr.getId()));
		assertEquals(User.getById(u.getId()).getEmail(), adrr.getAddress());
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

	@Test
	public void testMailDeleteWeb() throws InterruptedException, GigiApiException, MalformedURLException,
		UnsupportedEncodingException, IOException {
		EmailAddress addr = createVerifiedEmail(u);
		assertNull(executeBasicWebInteraction(cookie, path, "delete&delid[]=" + addr.getId(), 0));
		User u = User.getById(this.u.getId());
		EmailAddress[] addresses = u.getEmails();
		for (int i = 0; i < addresses.length; i++) {
			assertNotEquals(addresses[i].getAddress(), addr.getAddress());
		}
	}

	@Test
	public void testMailDeleteWebMulti() throws InterruptedException, GigiApiException, MalformedURLException,
		UnsupportedEncodingException, IOException {
		EmailAddress[] addr = new EmailAddress[] { createVerifiedEmail(u), createVerifiedEmail(u) };
		assertNull(executeBasicWebInteraction(cookie, path,
			"delete&delid[]=" + addr[0].getId() + "&delid[]="
			+ addr[1].getId(), 0));
		User u = User.getById(this.u.getId());
		EmailAddress[] addresses = u.getEmails();
		for (int i = 0; i < addresses.length; i++) {
			assertNotEquals(addresses[i].getAddress(), addr[0].getAddress());
			assertNotEquals(addresses[i].getAddress(), addr[1].getAddress());
		}
	}
}
