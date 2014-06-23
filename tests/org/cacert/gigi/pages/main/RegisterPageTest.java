package org.cacert.gigi.pages.main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.cacert.gigi.IOUtils;
import org.cacert.gigi.InitTruststore;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class RegisterPageTest {
	private static final URL registerService;
	static {
		URL u = null;
		try {
			u = new URL("https://localhost/register");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		registerService = u;
		InitTruststore.run();
		HttpURLConnection.setFollowRedirects(false);
	}

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testSuccess() throws IOException {
		String startError = fetchStartErrorMessage("fname=a&lname=b&email=e&pword1=ap&pword2=ap&day=1&month=1&year=1910&cca_agree=1");
		assertTrue(startError, startError.startsWith("</div>"));
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

	@Ignore
	@Test
	public void testDoubleMail() throws IOException {
		long uniq = System.currentTimeMillis();
		registerUser("RegisterTest", "User", "testmail" + uniq + "@cacert.org",
				"registerPW");
		try {
			registerUser("RegisterTest", "User", "testmail" + uniq
					+ "@cacert.org", "registerPW");
			throw new Error(
					"Registering a user with the same email needs to fail.");
		} catch (AssertionError e) {

		}
	}

	private static void testFailedForm(String query) throws IOException {
		String startError = fetchStartErrorMessage(query);
		assertTrue(startError, !startError.startsWith("</div>"));
	}
	private static String fetchStartErrorMessage(String query)
			throws IOException {
		String d = runRegister(query);
		String formFail = "<div class='formError'>";
		int idx = d.indexOf(formFail);
		assertNotEquals(-1, idx);
		String startError = d.substring(idx + formFail.length(), idx + 100)
				.trim();
		return startError;
	}

	public static void registerUser(String firstName, String lastName,
			String email, String password) {
		try {
			String query = "fname=" + URLEncoder.encode(firstName, "UTF-8")
					+ "&lname=" + URLEncoder.encode(lastName, "UTF-8")
					+ "&email=" + URLEncoder.encode(email, "UTF-8")
					+ "&pword1=" + URLEncoder.encode(password, "UTF-8")
					+ "&pword2=" + URLEncoder.encode(password, "UTF-8")
					+ "&day=1&month=1&year=1910&cca_agree=1";
			String data = fetchStartErrorMessage(query);
			assertTrue(data, data.startsWith("</div>"));
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}
	private static String runRegister(String param) throws IOException {
		HttpURLConnection uc = (HttpURLConnection) registerService
				.openConnection();
		uc.setDoOutput(true);
		uc.getOutputStream().write(param.getBytes());
		String d = IOUtils.readURL(uc);
		return d;
	}
}
