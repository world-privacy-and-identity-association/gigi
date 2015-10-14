package org.cacert.gigi.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.lambdaworks.crypto.SCryptUtil;

public class PasswordHash {

    /**
     * Verifies a password hash.
     * 
     * @param password
     *            The password that should result in the given hash.
     * @param hash
     *            The hash to verify the password against.
     * @return <ul>
     *         <li><code>null</code>, if the password was valid</li>
     *         <li><code>hash</code>, if the password is valid and the hash
     *         doesn't need to be updated</li>
     *         <li>a new hash, if the password is valid but the hash in the
     *         database needs to be updated.</li>
     *         </ul>
     */
    public static String verifyHash(String password, String hash) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        if (hash.contains("$")) {
            if (SCryptUtil.check(password, hash)) {
                return hash;
            } else {
                return null;
            }
        }
        String newhash = sha1(password);
        boolean match = true;
        if (newhash.length() != hash.length()) {
            match = false;
        }
        for (int i = 0; i < newhash.length(); i++) {
            match &= newhash.charAt(i) == hash.charAt(i);
        }
        if (match) {
            return hash(password);
        } else {
            return null;
        }
    }

    public static String sha1(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(password.getBytes("UTF-8"));
            StringBuffer res = new StringBuffer(digest.length * 2);
            for (int i = 0; i < digest.length; i++) {
                res.append(Integer.toHexString((digest[i] & 0xF0) >> 4));
                res.append(Integer.toHexString(digest[i] & 0xF));
            }
            return res.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public static String hash(String password) {
        return SCryptUtil.scrypt(password, 1 << 14, 8, 1);
    }
}
