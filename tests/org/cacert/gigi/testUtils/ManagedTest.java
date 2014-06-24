package org.cacert.gigi.testUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.cacert.gigi.DevelLauncher;
import org.cacert.gigi.testUtils.TestEmailReciever.TestMail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ManagedTest {
	private static TestEmailReciever ter;
	private static Process gigi;
	private static String url = "localhost:4443";

	public static String getServerName() {
		return url;
	}
	static Properties testProps = new Properties();
	@BeforeClass
	public static void connectToServer() {
		try {
			testProps.load(new FileInputStream("config/test.properties"));
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
			mainProps.load(new FileInputStream("config/gigi.properties"));
			mainProps.setProperty("host", "127.0.0.1");
			mainProps.setProperty("port", testProps.getProperty("serverPort"));
			mainProps.setProperty("emailProvider",
					"org.cacert.gigi.email.TestEmailProvider");
			mainProps.setProperty("emailProvider.port", "8473");

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
		ter.clearMails();
	}

	public TestMail waitForMail() {
		try {
			return ter.recieve();
		} catch (InterruptedException e) {
			throw new Error(e);
		}
	}
}
