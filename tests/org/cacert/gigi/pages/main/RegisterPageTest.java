package org.cacert.gigi.pages.main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Pattern;

import org.cacert.gigi.testUtils.InitTruststore;
import org.cacert.gigi.testUtils.ManagedTest;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.junit.Before;
import org.junit.Test;

public class RegisterPageTest extends ManagedTest {
	static {
		InitTruststore.run();
		HttpURLConnection.setFollowRedirects(false);
	}

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testSuccess() throws IOException {
		long uniq = System.currentTimeMillis();
		registerUser("ab", "b", "correct" + uniq + "@email.de", "ap12UI.'");
		TestMail tm = waitForMail();
		String link = tm.extractLink();
		assertTrue(link, link.startsWith("https://"));
	}
	@Test
	public void testNoFname() throws IOException {
		testFailedForm("lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&cca_agree=1");
	}
	@Test
	public void testNoLname() throws IOException {
		testFailedForm("fname=a&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&cca_agree=1");
	}
	@Test
	public void testNoEmail() throws IOException {
		testFailedForm("fname=a&lname=b&pword1=ap&pword2=ap&day=1&month=1&year=1910&cca_agree=1");
	}

	@Test
	public void testNoPword() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword2=ap&day=1&month=1&year=1910&cca_agree=1");
	}

	@Test
	public void testDiffPword() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap2&day=1&month=1&year=1910&cca_agree=1");
	}

	@Test
	public void testNoDay() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&month=1&year=1910&cca_agree=1");
	}
	@Test
	public void testNoMonth() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&year=1910&cca_agree=1");
	}
	@Test
	public void testNoYear() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&cca_agree=1");
	}
	@Test
	public void testInvDay() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=40&month=1&year=1910&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=0&month=1&year=1910&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=a&month=1&year=1910&cca_agree=1");
	}
	@Test
	public void testInvMonth() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=20&year=1910&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=0&year=1910&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=-1&year=1910&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=a&year=1910&cca_agree=1");
	}
	@Test
	public void testInvYear() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=0&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=100&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=a&cca_agree=1");
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=-1&cca_agree=1");
	}
	@Test
	public void testNoAgree() throws IOException {
		testFailedForm("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&cca_agree=a");
	}

	@Test
	public void testDataStays() throws IOException {
		long uniq = System.currentTimeMillis();
		String run = runRegister("fname=fn" + uniq + "&lname=ln" + uniq
				+ "&email=ma" + uniq + "@cacert.org&pword1=pas" + uniq
				+ "&pword2=pas2" + uniq + "&day=1&month=1&year=0");
		assertTrue(run.contains("fn" + uniq));
		assertTrue(run.contains("ln" + uniq));
		assertTrue(run.contains("ma" + uniq + "@cacert.org"));
		assertTrue(!run.contains("pas" + uniq));
		assertTrue(!run.contains("pas2" + uniq));

	}

	@Test
	public void testCheckboxesStay() throws IOException {
		String run2 = runRegister("general=1&country=a&regional=1&radius=0");
		assertTrue(run2
				.contains("name=\"general\" value=\"1\" checked=\"checked\">"));
		assertTrue(run2.contains("name=\"country\" value=\"1\">"));
		assertTrue(run2
				.contains("name=\"regional\" value=\"1\" checked=\"checked\">"));
		assertTrue(run2.contains("name=\"radius\" value=\"1\">"));
		run2 = runRegister("general=0&country=1&radius=1");
		assertTrue(run2.contains("name=\"general\" value=\"1\">"));
		assertTrue(run2
				.contains("name=\"country\" value=\"1\" checked=\"checked\">"));
		assertTrue(run2.contains("name=\"regional\" value=\"1\">"));
		assertTrue(run2
				.contains("name=\"radius\" value=\"1\" checked=\"checked\">"));
	}

	@Test
	public void testDoubleMail() throws IOException {
		long uniq = System.currentTimeMillis();
		registerUser("RegisterTest", "User", "testmail" + uniq + "@cacert.org",
				"registerPW'1");
		try {
			registerUser("RegisterTest", "User", "testmail" + uniq
					+ "@cacert.org", "registerPW");
			throw new Error(
					"Registering a user with the same email needs to fail.");
		} catch (AssertionError e) {

		}
	}
	@Test
	public void testInvalidMailbox() {
		getMailReciever().setApproveRegex(Pattern.compile("a"));
		long uniq = System.currentTimeMillis();
		try {
			registerUser("RegisterTest", "User", "testInvalidMailbox" + uniq
					+ "@cacert.org", "registerPW");
			throw new Error(
					"Registering a user with invalid mailbox must fail.");
		} catch (AssertionError e) {

		}
	}
	private void testFailedForm(String query) throws IOException {
		String startError = fetchStartErrorMessage(runRegister(query));
		assertTrue(startError, !startError.startsWith("</div>"));
	}

}
