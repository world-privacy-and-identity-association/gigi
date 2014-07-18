package org.cacert.gigi.testUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.cacert.gigi.DevelLauncher;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.cacert.gigi.util.DatabaseManager;
import org.cacert.gigi.util.SimpleSigner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ManagedTest {
	private final String registerService = "/register";

	private static TestEmailReciever ter;
	private static Process gigi;
	private static String url = "localhost:4443";

	public static String getServerName() {
		return url;
	}

	static Properties testProps = new Properties();
	static {
		InitTruststore.run();
		HttpURLConnection.setFollowRedirects(false);
	}

	@BeforeClass
	public static void connectToServer() {
		try {
			testProps.load(new FileInputStream("config/test.properties"));
			if (!DatabaseConnection.isInited()) {
				DatabaseConnection.init(testProps);
			}
			System.out.println("... purging Database");
			DatabaseManager.run(new String[] { testProps.getProperty("sql.driver"), testProps.getProperty("sql.url"),
					testProps.getProperty("sql.user"), testProps.getProperty("sql.password") });

			String type = testProps.getProperty("type");
			if (type.equals("local")) {
				url = testProps.getProperty("name.www") + ":" + testProps.getProperty("serverPort");
				String[] parts = testProps.getProperty("mail").split(":", 2);
				ter = new TestEmailReciever(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
				return;
			}
			url = testProps.getProperty("name.www") + ":" + testProps.getProperty("serverPort");
			gigi = Runtime.getRuntime().exec(testProps.getProperty("java"));
			DataOutputStream toGigi = new DataOutputStream(gigi.getOutputStream());
			System.out.println("... starting server");
			Properties mainProps = new Properties();
			mainProps.setProperty("host", "127.0.0.1");
			mainProps.setProperty("name.secure", testProps.getProperty("name.secure"));
			mainProps.setProperty("name.www", testProps.getProperty("name.www"));
			mainProps.setProperty("name.static", testProps.getProperty("name.static"));

			mainProps.setProperty("port", testProps.getProperty("serverPort"));
			mainProps.setProperty("emailProvider", "org.cacert.gigi.email.TestEmailProvider");
			mainProps.setProperty("emailProvider.port", "8473");
			mainProps.setProperty("sql.driver", testProps.getProperty("sql.driver"));
			mainProps.setProperty("sql.url", testProps.getProperty("sql.url"));
			mainProps.setProperty("sql.user", testProps.getProperty("sql.user"));
			mainProps.setProperty("sql.password", testProps.getProperty("sql.password"));

			byte[] cacerts = Files.readAllBytes(Paths.get("config/cacerts.jks"));
			byte[] keystore = Files.readAllBytes(Paths.get("config/keystore.pkcs12"));

			DevelLauncher.writeGigiConfig(toGigi, "changeit".getBytes(), "changeit".getBytes(), mainProps, cacerts,
				keystore);
			toGigi.flush();

			final BufferedReader br = new BufferedReader(new InputStreamReader(gigi.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null && !line.contains("Server:main: Started")) {
			}
			new Thread() {
				@Override
				public void run() {
					String line;
					try {
						while ((line = br.readLine()) != null) {
							System.err.println(line);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();
			if (line == null) {
				throw new Error("Server startup failed");
			}
			ter = new TestEmailReciever(new InetSocketAddress("localhost", 8473));
			SimpleSigner.runSigner();
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@AfterClass
	public static void tearDownServer() {
		String type = testProps.getProperty("type");
		ter.destroy();
		if (type.equals("local")) {
			return;
		}
		gigi.destroy();
		try {
			SimpleSigner.stopSigner();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@After
	public void removeMails() {
		ter.reset();
	}

	public TestMail waitForMail() {
		try {
			return ter.recieve();
		} catch (InterruptedException e) {
			throw new Error(e);
		}
	}

	public static TestEmailReciever getMailReciever() {
		return ter;
	}

	public String runRegister(String param) throws IOException {
		URL regist = new URL("https://" + getServerName() + registerService);
		HttpURLConnection uc = (HttpURLConnection) regist.openConnection();
		HttpURLConnection csrfConn = (HttpURLConnection) regist.openConnection();

		String headerField = csrfConn.getHeaderField("Set-Cookie");
		headerField = stripCookie(headerField);

		String csrf = getCSRF(csrfConn);
		uc.addRequestProperty("Cookie", headerField);
		uc.setDoOutput(true);
		uc.getOutputStream().write((param + "&csrf=" + csrf).getBytes());
		String d = IOUtils.readURL(uc);
		return d;
	}

	public String fetchStartErrorMessage(String d) throws IOException {
		String formFail = "<div class='formError'>";
		int idx = d.indexOf(formFail);
		assertNotEquals(-1, idx);
		String startError = d.substring(idx + formFail.length(), idx + 100).trim();
		return startError;
	}

	public void registerUser(String firstName, String lastName, String email, String password) {
		try {
			String query = "fname=" + URLEncoder.encode(firstName, "UTF-8") + "&lname="
				+ URLEncoder.encode(lastName, "UTF-8") + "&email=" + URLEncoder.encode(email, "UTF-8") + "&pword1="
				+ URLEncoder.encode(password, "UTF-8") + "&pword2=" + URLEncoder.encode(password, "UTF-8")
				+ "&day=1&month=1&year=1910&cca_agree=1";
			String data = fetchStartErrorMessage(runRegister(query));
			assertTrue(data, data.startsWith("</div>"));
		} catch (UnsupportedEncodingException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public int createVerifiedUser(String firstName, String lastName, String email, String password) {
		registerUser(firstName, lastName, email, password);
		try {
			TestMail tm = ter.recieve();
			String verifyLink = tm.extractLink();
			String[] parts = verifyLink.split("\\?");
			URL u = new URL("https://" + getServerName() + "/verify?" + parts[1]);
			u.openStream().close();
			;
			PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id FROM users where email=?");
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			throw new Error();
		} catch (InterruptedException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		} catch (SQLException e) {
			throw new Error(e);
		}
	}

	/**
	 * Creates a new user with 100 Assurance points given by an (invalid)
	 * assurance.
	 * 
	 * @param firstName
	 *            the first name
	 * @param lastName
	 *            the last name
	 * @param email
	 *            the email
	 * @param password
	 *            the password
	 * @return a new userid.
	 */
	public int createAssuranceUser(String firstName, String lastName, String email, String password) {
		int uid = createVerifiedUser(firstName, lastName, email, password);
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"INSERT INTO `cats_passed` SET `user_id`=?, `variant_id`=?");
			ps.setInt(1, uid);
			ps.setInt(2, 0);
			ps.execute();
			ps = DatabaseConnection.getInstance().prepare("INSERT INTO `notary` SET `from`=?, `to`=?, points='100'");
			ps.setInt(1, uid);
			ps.setInt(2, uid);
			ps.execute();

		} catch (SQLException e) {
			throw new Error(e);
		}
		return uid;
	}

	static int count = 0;

	public String createUniqueName() {
		return "test" + System.currentTimeMillis() + "a" + (count++);
	}

	private String stripCookie(String headerField) {
		return headerField.substring(0, headerField.indexOf(';'));
	}

	public String login(String email, String pw) throws IOException {
		URL u = new URL("https://" + getServerName() + "/login");
		HttpURLConnection huc = (HttpURLConnection) u.openConnection();
		huc.setDoOutput(true);
		OutputStream os = huc.getOutputStream();
		String data = "username=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(pw, "UTF-8");
		os.write(data.getBytes());
		os.flush();
		String headerField = huc.getHeaderField("Set-Cookie");
		return stripCookie(headerField);
	}

	public String login(final PrivateKey pk, final X509Certificate ce) throws NoSuchAlgorithmException,
		KeyManagementException, IOException, MalformedURLException {
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
		if (connection.getResponseCode() == 302) {
			assertEquals("https://" + getServerName().replaceFirst("^www.", "secure.").replaceFirst(":443$", "") + "/",
				connection.getHeaderField("Location").replaceFirst(":443$", ""));
			return stripCookie(connection.getHeaderField("Set-Cookie"));
		} else {
			return null;
		}
	}

	public String getCSRF(URLConnection u) throws IOException {
		String content = IOUtils.readURL(u);
		Pattern p = Pattern.compile("<input type='hidden' name='csrf' value='([^']+)'>");
		Matcher m = p.matcher(content);
		if (!m.find()) {
			throw new Error("No CSRF Token");
		}
		return m.group(1);
	}

	public static String[] generateCSR(String dn) throws IOException {
		Process p = Runtime.getRuntime().exec(
			new String[] { "openssl", "req", "-newkey", "rsa:1024", "-nodes", "-subj", dn, "-config",
					"keys/selfsign.config" });
		String csr = IOUtils.readURL(new InputStreamReader(p.getInputStream()));

		String[] parts = csr.split("(?<=-----)\n(?=-----)");
		if (parts.length != 2) {
			System.err.println(IOUtils.readURL(new InputStreamReader(p.getErrorStream())));
			throw new Error();
		}
		return parts;
	}

}
