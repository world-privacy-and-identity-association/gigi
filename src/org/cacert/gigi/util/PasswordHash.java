package org.cacert.gigi.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHash {
	public static boolean verifyHash(String password, String hash) {
		String newhash = sha1(password);
		return newhash.equals(hash);
	}

	private static String sha1(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] digest = md.digest(password.getBytes());
			StringBuffer res = new StringBuffer(digest.length * 2);
			for (int i = 0; i < digest.length; i++) {
				res.append(Integer.toHexString((digest[i] & 0xF0) >> 4));
				res.append(Integer.toHexString(digest[i] & 0xF));
			}
			return res.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		}
	}

	public static String hash(String password) {
		return sha1(password);
	}
}
