package org.cacert.gigi.util;

import java.util.Properties;

public class ServerConstants {

    private static String wwwHostName = "www.cacert.local";

    private static String secureHostName = "secure.cacert.local";

    private static String staticHostName = "static.cacert.local";

    private static String apiHostName = "api.cacert.local";

    private static String port;

    public static void init(Properties conf) {
        port = "";
        if ( !conf.getProperty("port").equals("443")) {
            port = ":" + conf.getProperty("port");
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
        return secureHostName + port;
    }

    public static String getStaticHostNamePort() {
        return staticHostName + port;
    }

    public static String getWwwHostNamePort() {
        return wwwHostName + port;
    }

    public static String getApiHostNamePort() {
        return apiHostName + port;
    }

}
