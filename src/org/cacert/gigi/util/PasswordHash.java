package org.cacert.gigi.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.lambdaworks.crypto.SCryptUtil;

public class PasswordHash {

    public static boolean verifyHash(String password, String hash) {
        if (hash.contains("$")) {
            return SCryptUtil.check(password, hash);
        }
        String newhash = sha1(password);
        boolean match = true;
        if (newhash.length() != hash.length()) {
            match = false;
        }
        for (int i = 0; i < newhash.length(); i++) {
            match &= newhash.charAt(i) == hash.charAt(i);
        }
        return match;
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
        return SCryptUtil.scrypt(password, 1 << 14, 8, 1);
    }
}
