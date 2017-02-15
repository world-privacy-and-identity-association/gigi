package club.wpia.gigi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.IDN;
import java.util.HashSet;

public class PublicSuffixes {

    private HashSet<String> suffixes = new HashSet<>();

    private HashSet<String> wildcards = new HashSet<>();

    private HashSet<String> exceptions = new HashSet<>();

    static final String url = "https://publicsuffix.org/list/effective_tld_names.dat";

    private static PublicSuffixes instance;

    private static PublicSuffixes generateDefault() throws IOException {
        InputStream res = PublicSuffixes.class.getResourceAsStream("effective_tld_names.dat");

        if (null == res) {
            throw new Error("Public Suffix List could not be loaded.");
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(res, "UTF-8"))) {
            return new PublicSuffixes(br);
        }
    }

    public synchronized static PublicSuffixes getInstance() {
        if (instance == null) {
            try {
                instance = generateDefault();
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        return instance;
    }

    private PublicSuffixes(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("//")) {
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String[] lineParts = line.split("\\s", 2);
            if (lineParts.length == 0) {
                throw new Error("split had strange behavior");
            }
            line = lineParts[0];
            if (line.startsWith("*.")) {
                String data = line.substring(2);
                if (data.contains("*") || data.contains("!")) {
                    System.out.println("Error! unparsable public suffix line: " + line);
                    continue;
                }
                addWildcard(IDN.toASCII(data));
            } else if (line.startsWith("!")) {
                String data = line.substring(1);
                if (data.contains("*") || data.contains("!")) {
                    System.out.println("Error! unparsable public suffix line: " + line);
                    continue;
                }
                addException(IDN.toASCII(data));
            } else {
                if (line.contains("*") || line.contains("!")) {
                    System.out.println("Error! unparsable public suffix line: " + line);
                    continue;
                }
                addSuffix(IDN.toASCII(line));
            }
        }
    }

    private void addWildcard(String data) {
        wildcards.add(data);
    }

    private void addException(String data) {
        exceptions.add(data);
    }

    private void addSuffix(String line) {
        suffixes.add(line);
    }

    public String getRegistrablePart(String domain) {
        if (domain == null) {
            return null;
        }
        if (domain.startsWith(".")) {
            return null;
        }
        if (isSuffix(domain) && !exceptions.contains(domain)) {
            return null;
        }
        return getPublicSuffix0(domain);
    }

    private String getPublicSuffix0(String domain) {

        int d = domain.indexOf('.');
        if (d == -1) {
            return null;
        }
        if (exceptions.contains(domain)) {
            return domain;
        }
        String nextDomain = domain.substring(d + 1);
        if (isSuffix(nextDomain)) {
            return domain;
        }

        return getPublicSuffix0(nextDomain);
    }

    private boolean isSuffix(String domain) {
        if (suffixes.contains(domain)) {
            return true;
        }
        if (exceptions.contains(domain)) {
            return false;
        }
        int idx = domain.indexOf('.');
        if (idx != -1 && wildcards.contains(domain.substring(idx + 1))) {
            return true;
        }
        return false;
    }

}
