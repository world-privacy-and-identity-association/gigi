package org.cacert.gigi.util;

import java.util.Base64;
import java.util.regex.Pattern;

public class PEM {

    public static final Pattern LINE = Pattern.compile("(.{64})(?=.)");

    public static String encode(String type, byte[] data) {
        return "-----BEGIN " + type + "-----\n" + //
                formatBase64(data) + //
                "\n-----END " + type + "-----";
    }

    public static byte[] decode(String type, String data) {
        data = data.replaceAll("-----BEGIN " + type + "-----", "").replace("\n", "").replace("\r", "");
        // Remove the first and last lines
        data = data.replaceAll("-----END " + type + "-----", "");
        // Base64 decode the data
        return Base64.getDecoder().decode(data);

    }

    public static String formatBase64(byte[] bytes) {
        return LINE.matcher(Base64.getEncoder().encodeToString(bytes)).replaceAll("$1\n");
    }
}
