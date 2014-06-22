package org.cacert.gigi;

public class InitTruststore {
	private InitTruststore() {
	}
	static {
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		System.setProperty("javax.net.ssl.trustStore", "config/cacerts.jks");
	}
	public static void run() {

	}
}
