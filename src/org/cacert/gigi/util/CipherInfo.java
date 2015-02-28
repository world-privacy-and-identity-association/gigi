package org.cacert.gigi.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

import sun.security.ssl.SSLContextImpl;

public class CipherInfo implements Comparable<CipherInfo> {

    private static class CipherInfoGenerator {

        private Class<?> cipherSuite;

        private Field cipherSuiteNameMap;

        private Field exchange;

        private Field cipher;

        private Field keySize;

        private Field algortihm;

        private Field transformation;

        private HashMap<?, ?> names;

        private Field macAlg;

        private Field macName;

        private Field macSize;

        public CipherInfoGenerator() throws ReflectiveOperationException {
            SSLContextImpl sc = new SSLContextImpl.TLS12Context();
            Method m = SSLContextImpl.class.getDeclaredMethod("getSupportedCipherSuiteList");
            m.setAccessible(true);
            Object o = m.invoke(sc);
            Class<?> cipherSuiteList = o.getClass();
            Method collection = cipherSuiteList.getDeclaredMethod("collection");
            collection.setAccessible(true);
            Collection<?> suites = (Collection<?>) collection.invoke(o);
            Object oneSuite = suites.iterator().next();
            cipherSuite = oneSuite.getClass();
            cipherSuiteNameMap = cipherSuite.getDeclaredField("nameMap");
            cipherSuiteNameMap.setAccessible(true);
            names = (HashMap<?, ?>) cipherSuiteNameMap.get(null);
            exchange = cipherSuite.getDeclaredField("keyExchange");
            exchange.setAccessible(true);
            cipher = cipherSuite.getDeclaredField("cipher");
            cipher.setAccessible(true);
            Class<?> bulkCipher = cipher.getType();
            keySize = bulkCipher.getDeclaredField("keySize");
            keySize.setAccessible(true);
            algortihm = bulkCipher.getDeclaredField("algorithm");
            algortihm.setAccessible(true);
            transformation = bulkCipher.getDeclaredField("transformation");
            transformation.setAccessible(true);

            macAlg = cipherSuite.getDeclaredField("macAlg");
            macAlg.setAccessible(true);
            Class<?> mac = macAlg.getType();
            macName = mac.getDeclaredField("name");
            macName.setAccessible(true);
            macSize = mac.getDeclaredField("size");
            macSize.setAccessible(true);
        }

        public CipherInfo generateInfo(String suiteName) throws IllegalArgumentException, IllegalAccessException {
            Object suite = names.get(suiteName);
            String keyExchange = exchange.get(suite).toString();
            Object bulkCipher = cipher.get(suite);
            Object mac = macAlg.get(suite);

            String transform = (String) transformation.get(bulkCipher);
            String[] transformationParts = transform.split("/");
            int keysize = keySize.getInt(bulkCipher);

            String macNam = (String) macName.get(mac);
            int macSiz = macSize.getInt(mac);

            String chaining = null;
            String padding = null;
            if (transformationParts.length > 1) {
                chaining = transformationParts[1];
                padding = transformationParts[2];
            }

            return new CipherInfo(suiteName, keyExchange, transformationParts[0], keysize * 8, chaining, padding, macNam, macSiz * 8);

        }
    }

    String keyExchange;

    String cipher;

    int keySize;

    String cipherChaining;

    String cipherPadding;

    String macName;

    int macSize;

    String suiteName;

    private CipherInfo(String suiteName, String keyExchange, String cipher, int keySize, String cipherChaining, String cipherPadding, String macName, int macSize) {
        this.suiteName = suiteName;
        this.keyExchange = keyExchange;
        this.cipher = cipher;
        this.keySize = keySize;
        this.cipherChaining = cipherChaining;
        this.cipherPadding = cipherPadding;
        this.macName = macName;
        this.macSize = macSize;
    }

    static CipherInfoGenerator cig;
    static {
        try {
            cig = new CipherInfoGenerator();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static CipherInfo generateInfo(String name) {
        if (cig == null) {
            return null;
        }
        try {
            return cig.generateInfo(name);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSuiteName() {
        return suiteName;
    }

    /**
     * 5: ECDHE, AES||CAMELLIA, keysize >=256 <br>
     * 4: DHE, AES||CAMELLIA, keysize >= 256<br>
     * 3: ECDHE|| DHE, AES||CAMELLIA<br>
     * 2: ECDHE||DHE<br>
     * 1: RSA||DSA <br>
     * 0: Others
     * 
     * @return the strength
     */
    public int getStrength() {
        if (cipher.equals("NULL") || cipher.equals("RC4") || cipher.contains("DES")) {
            return 0;
        }
        boolean ecdhe = keyExchange.startsWith("ECDHE");
        boolean dhe = keyExchange.startsWith("DHE");
        boolean pfs = ecdhe || dhe;
        boolean goodCipher = cipher.equals("AES") || cipher.equals("CAMELLIA");
        if (ecdhe && goodCipher && keySize >= 256) {
            return 5;
        }
        if (dhe && goodCipher && keySize >= 256) {
            return 4;
        }
        if (pfs && goodCipher) {
            return 3;
        }
        if (pfs) {
            return 2;
        }
        if (keyExchange.equals("RSA") || keyExchange.equals("DSA")) {
            return 1;
        }
        return 0;
    }

    private static final String[] CIPHER_RANKING = new String[] {
            "CAMELLIA", "AES", "RC4", "3DES", "DES", "DES40"
    };

    @Override
    public String toString() {
        return "CipherInfo [keyExchange=" + keyExchange + ", cipher=" + cipher + ", keySize=" + keySize + ", cipherChaining=" + cipherChaining + ", cipherPadding=" + cipherPadding + ", macName=" + macName + ", macSize=" + macSize + "]";
    }

    /**
     * ECDHE<br>
     * GCM<br>
     * Cipher {@link #CIPHER_RANKING}<br>
     * Cipher {@link #keySize}<br>
     * HMAC<br>
     * HMAC size<br>
     * 
     * @return
     */
    @Override
    public int compareTo(CipherInfo o) {
        int myStrength = getStrength();
        int oStrength = o.getStrength();
        if (myStrength > oStrength) {
            return -1;
        }
        if (myStrength < oStrength) {
            return 1;
        }
        // TODO sort SSL/TLS
        boolean myEcdhe = keyExchange.startsWith("ECDHE");
        boolean oEcdhe = o.keyExchange.startsWith("ECDHE");
        if (myEcdhe && !oEcdhe) {
            return -1;
        }
        if ( !myEcdhe && oEcdhe) {
            return 1;
        }
        boolean myGCM = "GCM".equals(cipherChaining);
        boolean oGCM = "GCM".equals(o.cipherChaining);
        if (myGCM && !oGCM) {
            return -1;
        }
        if ( !myGCM && oGCM) {
            return 1;
        }
        if ( !cipher.equals(o.cipher)) {

            for (String testCipher : CIPHER_RANKING) {
                if (cipher.equals(testCipher)) {
                    return -1;
                }
                if (o.cipher.equals(testCipher)) {
                    return 1;
                }
            }
            if (cipher.equals("NULL")) {
                return 1;
            }
            if (o.cipher.equals("NULL")) {
                return -1;
            }
        }
        if (keySize > o.keySize) {
            return -1;
        }
        if (keySize < o.keySize) {
            return 1;
        }
        boolean mySHA = macName.startsWith("SHA");
        boolean oSHA = o.macName.startsWith("SHA");
        if ( !mySHA && oSHA) {
            return -1;
        }
        if (mySHA && !oSHA) {
            return 1;
        }
        if (macSize > o.macSize) {
            return -1;
        }
        if (macSize < o.macSize) {
            return 1;
        }

        return suiteName.compareTo(o.suiteName);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CipherInfo) {
            return 0 == this.compareTo((CipherInfo) o);
        }

        return false;
    }

    static String[] cipherRanking = null;

    public static String[] getCompleteRanking() {
        if (cipherRanking == null) {
            String[] ciphers = filterCiphers((Iterable<String>) cig.names.keySet());
            cipherRanking = ciphers;
        }
        return cipherRanking;
    }

    private static String[] filterCiphers(Iterable<String> toFilter) {
        TreeSet<CipherInfo> chosenCiphers = new TreeSet<CipherInfo>();
        for (String o : toFilter) {
            String s = o;
            CipherInfo info = CipherInfo.generateInfo(s);
            if (info != null) {
                if (info.getStrength() > 1) {
                    chosenCiphers.add(info);
                }
            }
        }
        String[] ciphers = new String[chosenCiphers.size()];
        int counter = 0;
        for (CipherInfo i : chosenCiphers) {
            ciphers[counter++] = i.getSuiteName();
        }
        return ciphers;
    }

    public static String[] filter(String[] supportedCipherSuites) {
        return filterCiphers(Arrays.asList(supportedCipherSuites));
    }
}
