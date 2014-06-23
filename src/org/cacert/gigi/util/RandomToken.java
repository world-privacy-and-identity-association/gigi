package org.cacert.gigi.util;

import java.security.SecureRandom;

public class RandomToken {
	static SecureRandom sr = new SecureRandom();
	public static String generateToken(int length) {
		StringBuffer token = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int rand = sr.nextInt(26 * 2 + 10);
			if (rand < 10) {
				token.append('0' + rand);
			}
			rand -= 10;
			if (rand < 26) {
				token.append('a' + rand);
			}
			rand -= 26;
			token.append('A' + rand);
		}
		return token.toString();
	}
}
