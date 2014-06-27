package org.cacert.gigi.testUtils;

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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.cacert.gigi.DevelLauncher;
import org.cacert.gigi.IOUtils;
import org.cacert.gigi.InitTruststore;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.cacert.gigi.util.DatabaseManager;
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
			DatabaseManager.run(new String[]{
					testProps.getProperty("sql.driver"),
					testProps.getProperty("sql.url"),
					testProps.getProperty("sql.user"),
					testProps.getProperty("sql.password")});

			String type = testProps.getProperty("type");
			if (type.equals("local")) {
				url = testProps.getProperty("server");
				String[] parts = testProps.getProperty("mail").split(":", 2);
				ter = new TestEmailReciever(new InetSocketAddress(parts[0],
						Integer.parseInt(parts[1])));
				return;
			}
			url = "localhost:" + testProps.getProperty("serverPort");
			gigi = Runtime.getRuntime().exec(testProps.getProperty("java"));
			DataOutputStream toGigi = new DataOutputStream(
					gigi.getOutputStream());
			System.out.println("... starting server");
			Properties mainProps = new Properties();
			mainProps.setProperty("host", "127.0.0.1");
			mainProps.setProperty("port", testProps.getProperty("serverPort"));
			mainProps.setProperty("emailProvider",
					"org.cacert.gigi.email.TestEmailProvider");
			mainProps.setProperty("emailProvider.port", "8473");
			mainProps.setProperty("sql.driver",
					testProps.getProperty("sql.driver"));
			mainProps.setProperty("sql.url", testProps.getProperty("sql.url"));
			mainProps
					.setProperty("sql.user", testProps.getProperty("sql.user"));
			mainProps.setProperty("sql.password",
					testProps.getProperty("sql.password"));

			byte[] cacerts = Files
					.readAllBytes(Paths.get("config/cacerts.jks"));
			byte[] keystore = Files.readAllBytes(Paths
					.get("config/keystore.pkcs12"));

			DevelLauncher.writeGigiConfig(toGigi, new byte[]{},
					"changeit".getBytes(), mainProps, cacerts, keystore);
			toGigi.flush();
			// TODO wait for ready
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			final BufferedReader br = new BufferedReader(new InputStreamReader(
					gigi.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null
					&& !line.contains("Server:main: Started")) {
				System.err.println(line);
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
			System.err.println(line);
			if (line == null) {
				throw new Error("Server startup failed");
			}
			ter = new TestEmailReciever(
					new InetSocketAddress("localhost", 8473));
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

	}
	@AfterClass
	public static void tearDownServer() {
		String type = testProps.getProperty("type");
		if (type.equals("local")) {
			return;
		}
		gigi.destroy();
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
		HttpURLConnection uc = (HttpURLConnection) new URL("https://"
				+ getServerName() + registerService).openConnection();
		uc.setDoOutput(true);
		uc.getOutputStream().write(param.getBytes());
		String d = IOUtils.readURL(uc);
		return d;
	}
	public String fetchStartErrorMessage(String query) throws IOException {
		String d = runRegister(query);
		String formFail = "<div class='formError'>";
		int idx = d.indexOf(formFail);
		assertNotEquals(-1, idx);
		String startError = d.substring(idx + formFail.length(), idx + 100)
				.trim();
		return startError;
	}

	public void registerUser(String firstName, String lastName, String email,
			String password) {
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
	public int createVerifiedUser(String firstName, String lastName,
			String email, String password) {
		registerUser(firstName, lastName, email, password);
		try {
			TestMail tm = ter.recieve();
			String verifyLink = tm.extractLink();
			String[] parts = verifyLink.split("\\?");
			URL u = new URL("https://" + getServerName() + "/verify?"
					+ parts[1]);
			u.openStream().close();;
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"SELECT id FROM users where email=?");
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
	public int createAssuranceUser(String firstName, String lastName,
			String email, String password) {
		int uid = createVerifiedUser(firstName, lastName, email, password);
		// TODO make him pass CATS and be assured for 100 points
		return uid;
	}
	static int count = 0;
	public String createUniqueName() {
		return "test" + System.currentTimeMillis() + "a" + (count++);
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
