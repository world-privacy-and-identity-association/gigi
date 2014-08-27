package org.cacert.gigi.util;

import java.util.Properties;

public class ServerConstants {

    private static String wwwHostName = "www.cacert.local";

    private static String secureHostName = "secure.cacert.local";

    private static String staticHostName = "static.cacert.local";

    private static String apiHostName = "api.cacert.local";

    private static String securePort, port;

    public static void init(Properties conf) {
        securePort = port = "";
        if ( !conf.getProperty("https.port").equals("443")) {
            securePort = ":" + conf.getProperty("https.port");
        }
        if ( !conf.getProperty("http.port").equals("80")) {
            port = ":" + conf.getProperty("http.port");
        }
        wwwHostName = conf.getProperty("name.www");
        secureHostName = conf.getProperty("name.secure");
        staticHostName = conf.getProperty("name.static");
        apiHostName = conf.getProperty("name.api");

    }

    public static String getSecureHostName() {
        return secureHostName;
    }

    public static String getStaticHostName() {
        return staticHostName;
    }

    public static String getWwwHostName() {
        return wwwHostName;
    }

    public static String getApiHostName() {
        return apiHostName;
    }

    public static String getSecureHostNamePort() {
        return secureHostName + securePort;
    }

    public static String getStaticHostNamePortSecure() {
        return staticHostName + securePort;
    }

    public static String getWwwHostNamePortSecure() {
        return wwwHostName + securePort;
    }

    public static String getStaticHostNamePort() {
        return staticHostName + port;
    }

    public static String getWwwHostNamePort() {
        return wwwHostName + port;
    }

    public static String getApiHostNamePort() {
        return apiHostName + securePort;
    }

    public static int getSecurePort() {
        if (securePort.isEmpty()) {
            return 443;
        }
        return Integer.parseInt(securePort.substring(1, securePort.length()));
    }

    public static int getPort() {
        if (port.isEmpty()) {
            return 80;
        }
        return Integer.parseInt(port.substring(1, port.length()));
    }

}
