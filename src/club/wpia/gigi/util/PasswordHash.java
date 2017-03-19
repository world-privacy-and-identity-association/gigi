package club.wpia.gigi.util;

import java.util.Properties;

import com.lambdaworks.crypto.SCryptUtil;

public class PasswordHash {

    /**
     * Verifies a password hash.
     * 
     * @param password
     *            The password that should result in the given hash.
     * @param hash
     *            The hash to verify the password against.
     * @return
     *         <ul>
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

        return null;
    }

    public static String hash(String password) {
        return SCryptUtil.scrypt(password, N, r, p);
    }

    private static int N = 1 << 14;

    private static int r = 8;

    private static int p = 1;

    private static boolean initialized = false;

    public static synchronized void init(Properties prop) {
        if (initialized) {
            throw new IllegalStateException("Already initialized.");
        }
        String val = prop.getProperty("scrypt.params", "14;8;1");
        String[] parts = val.split(";", 3);
        int N = 1 << Integer.parseInt(parts[0]);
        int r = Integer.parseInt(parts[1]);
        int p = Integer.parseInt(parts[2]);
        checkScryptParams(N, r, p);
        PasswordHash.N = N;
        PasswordHash.r = r;
        PasswordHash.p = p;
        initialized = true;
    }

    private static void checkScryptParams(int N, int r, int p) {
        if (N < 2 || (N & (N - 1)) != 0) {
            throw new IllegalArgumentException("N must be a power of 2 greater than 1");
        }
        if (r <= 0) {
            throw new IllegalArgumentException("Parameter r zero or negative");
        }
        if (p <= 0) {
            throw new IllegalArgumentException("Parameter p zero or negative");
        }

        if (N > Integer.MAX_VALUE / 128 / r) {
            throw new IllegalArgumentException("Parameter N is too large");
        }
        if (r > Integer.MAX_VALUE / 128 / p) {
            throw new IllegalArgumentException("Parameter r is too large");
        }
    }
}
