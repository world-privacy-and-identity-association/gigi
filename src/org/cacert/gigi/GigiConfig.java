package org.cacert.gigi;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Properties;

public class GigiConfig {
	public static final String GIGI_CONFIG_VERSION = "GigiConfigV1.0";
	byte[] cacerts;
	byte[] keystore;
	Properties mainProps = new Properties();
	private char[] keystorpw;
	private char[] truststorepw;

	private GigiConfig() {
	}
	public byte[] getCacerts() {
		return cacerts;
	}
	public byte[] getKeystore() {
		return keystore;
	}
	public Properties getMainProps() {
		return mainProps;
	}

	public static GigiConfig parse(InputStream input) throws IOException {
		DataInputStream dis = new DataInputStream(input);
		String version = new String(readChunk(dis));
		if (!version.equals(GIGI_CONFIG_VERSION)) {
			System.out.println("Invalid config format");
			System.exit(0);
		}
		GigiConfig gc = new GigiConfig();
		gc.keystorpw = transformSafe(readChunk(dis));
		gc.truststorepw = transformSafe(readChunk(dis));
		gc.mainProps.load(new ByteArrayInputStream(readChunk(dis)));
		gc.cacerts = readChunk(dis);
		gc.keystore = readChunk(dis);
		return gc;
	}
	private static char[] transformSafe(byte[] readChunk) {
		char[] res = new char[readChunk.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = (char) readChunk[i];
			readChunk[i] = 0;
		}
		return res;
	}
	private static byte[] readChunk(DataInputStream dis) throws IOException {
		int length = dis.readInt();
		byte[] contents = new byte[length];
		dis.readFully(contents);
		return contents;
	}
	public KeyStore getPrivateStore() throws GeneralSecurityException,
			IOException {
		KeyStore ks1 = KeyStore.getInstance("pkcs12");
		ks1.load(new ByteArrayInputStream(keystore), keystorpw);
		return ks1;
	}
	public KeyStore getTrustStore() throws GeneralSecurityException,
			IOException {
		KeyStore ks1 = KeyStore.getInstance("jks");
		ks1.load(new ByteArrayInputStream(cacerts), truststorepw);
		return ks1;
	}
}
