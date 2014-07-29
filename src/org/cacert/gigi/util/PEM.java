package org.cacert.gigi.util;

import java.util.Base64;

public class PEM {

    public static String encode(String type, byte[] data) {
        return "-----BEGIN " + type + "-----\n" + //
                Base64.getEncoder().encodeToString(data).replaceAll("(.{64})(?=.)", "$1\n") + //
                "\n-----END " + type + "-----";
    }

    public static byte[] decode(String type, String data) {
        data = data.replaceAll("-----BEGIN " + type + "-----", "").replace("\n", "").replace("\r", "");
        // Remove the first and last lines
        data = data.replaceAll("-----END " + type + "-----", "");
        // Base64 decode the data
        return Base64.getDecoder().decode(data);

    }
}
