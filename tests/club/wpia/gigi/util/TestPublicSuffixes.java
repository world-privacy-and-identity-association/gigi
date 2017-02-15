package club.wpia.gigi.util;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.IDN;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import club.wpia.gigi.util.PublicSuffixes;

@RunWith(Parameterized.class)
public class TestPublicSuffixes {

    /**
     * Taken from
     * http://mxr.mozilla.org/mozilla-central/source/netwerk/test/unit
     * /data/test_psl.txt?raw=1
     */
    @Parameters(name = "publicSuffix({0}) = {1}")
    public static Iterable<String[]> genParams() throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(TestPublicSuffixes.class.getResourceAsStream("TestPublicSuffixes.txt"), "UTF-8"));
            ArrayList<String[]> result = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("//") || line.isEmpty()) {
                    continue;
                }
                String parseSuffix = "checkPublicSuffix(";
                if (line.startsWith(parseSuffix)) {
                    String data = line.substring(parseSuffix.length(), line.length() - 2);
                    String[] parts = data.split(", ");
                    if (parts.length != 2) {
                        throw new Error("Syntax error in public suffix test data file: " + line);
                    }
                    result.add(new String[] {
                            parse(parts[0]), parse(parts[1])
                    });
                } else {
                    throw new Error("Unparsable line: " + line);
                }
            }
            return result;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    private static String parse(String data) {
        if (data.equals("null")) {
            return null;
        }
        if (data.startsWith("'") && data.endsWith("'")) {
            return data.substring(1, data.length() - 1);
        }
        throw new Error("Syntax error with literal: " + data);
    }

    @Parameter(0)
    public String domain;

    @Parameter(1)
    public String suffix;

    @Test
    public void testPublicSuffix() {
        if (domain != null) {
            domain = domain.toLowerCase();
        }
        String publicSuffix = PublicSuffixes.getInstance().getRegistrablePart(domain == null ? null : IDN.toASCII(domain));
        assertEquals(suffix == null ? null : IDN.toASCII(suffix), publicSuffix);
    }
}
