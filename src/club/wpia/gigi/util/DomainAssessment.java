package club.wpia.gigi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.IDN;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import club.wpia.gigi.GigiApiException;

public class DomainAssessment {

    private static class DomainSet {

        private final Set<String> data;

        public DomainSet(URL u) {
            this(openStream(u));
        }

        private static Reader openStream(URL u) {
            try {
                return new InputStreamReader(u.openStream(), "UTF-8");
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        public DomainSet(Reader r) {
            HashSet<String> contents = new HashSet<>();
            try {
                BufferedReader br = new BufferedReader(r);
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    if (line.isEmpty()) {
                        continue;
                    }
                    contents.add(line);
                }
            } catch (IOException e) {
                throw new Error(e);
            }
            data = Collections.unmodifiableSet(contents);
        }

        public boolean contains(String suffix) {
            while (suffix.contains(".")) {
                if (data.contains(suffix)) {
                    return true;
                }
                suffix = suffix.substring(suffix.indexOf('.') + 1);
            }
            return data.contains(suffix);
        }
    }

    private static DomainSet financial;

    private static final DomainSet idn_enabled = new DomainSet(DomainAssessment.class.getResource("idn_enabled.dat"));

    public static boolean isHighFinancialValue(String suffix) {
        return financial.contains(suffix);
    }

    public static void checkCertifiableDomain(String domain, boolean hasPunycodeRight, boolean asRegister) throws GigiApiException {
        if (isHighFinancialValue(domain)) {
            throw new GigiApiException("Domain blocked for automatic adding.");
        }
        String[] parts = domain.split("\\.", -1);
        if (parts.length < 2) {
            throw new GigiApiException("Domain does not contain '.'.");
        }

        boolean neededPunycode = false;
        for (int i = parts.length - 1; i >= 0; i--) {
            if ( !isValidDomainPart(parts[i])) {
                throw new GigiApiException("Syntax error in Domain.");
            }
            boolean canBePunycode = parts[i].length() >= 4 && parts[i].charAt(2) == '-' && parts[i].charAt(3) == '-';
            if (canBePunycode) {
                if ( !hasPunycodeRight) {
                    throw new GigiApiException("Punycode domain without specific right.");
                }
                punycodeDecode(parts[i]);
                neededPunycode = true;
            }

        }
        if (neededPunycode && !idn_enabled.contains(domain)) {
            throw new GigiApiException("Punycode not allowed under this TLD.");
        }

        if (asRegister) {
            String publicSuffix = PublicSuffixes.getInstance().getRegistrablePart(domain);
            if ( !domain.equals(publicSuffix)) {
                throw new GigiApiException("You may only register a domain with exactly one label before the public suffix.");
            }
        }

        if (("." + domain).matches("(\\.[0-9]*)*")) {
            // This is not reached because we currently have no TLD that is
            // numbers only. But who knows..
            // Better safe than sorry.
            throw new GigiApiException("IP Addresses are not allowed.");
        }
    }

    private static String punycodeDecode(String label) throws GigiApiException {
        if (label.charAt(2) != '-' || label.charAt(3) != '-') {
            return label; // is no punycode
        }
        if ( !label.startsWith("xn--")) {
            throw new GigiApiException("Unknown ACE prefix.");
        }
        try {
            String unicode = IDN.toUnicode(label);
            if (unicode.startsWith("xn--")) {
                throw new GigiApiException("Punycode label could not be positively verified.");
            }
            return unicode;
        } catch (IllegalArgumentException e) {
            throw new GigiApiException("Punycode label could not be positively verified.");
        }
    }

    public static boolean isValidDomainPart(String s) {
        if ( !s.matches("[a-z0-9-]+")) {
            return false;
        }
        if (s.charAt(0) == '-' || s.charAt(s.length() - 1) == '-') {
            return false;
        }
        if (s.length() > 63) {
            return false;
        }
        return true;
    }

    public static void init(Properties conf) {
        String financialName = conf.getProperty("highFinancialValue");
        if (financialName == null) {
            throw new Error("No property highFinancialValue was configured");
        }
        try {
            financial = new DomainSet(new InputStreamReader(new FileInputStream(new File(financialName)), "UTF-8"));
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
