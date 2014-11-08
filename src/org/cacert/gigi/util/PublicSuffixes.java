package org.cacert.gigi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.URL;
import java.util.HashSet;

public class PublicSuffixes {

    HashSet<String> suffixes = new HashSet<>();

    HashSet<String> wildcards = new HashSet<>();

    HashSet<String> exceptions = new HashSet<>();

    private static final String url = "https://publicsuffix.org/list/effective_tld_names.dat";

    private static PublicSuffixes instance;

    private static void generateDefault() throws IOException {
        URL u = new URL(url);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream(), "UTF-8"));
        instance = new PublicSuffixes(br);
    }

    public static PublicSuffixes getInstance() {
        if (instance == null) {
            try {
                generateDefault();
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
            line = line.split("\\s", 2)[0];
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
