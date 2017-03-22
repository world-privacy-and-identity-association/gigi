package club.wpia.gigi.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ServerConstants {

    public enum Host {
        /**
         * Serves the main application. Both via HTTP and HTTPS.
         */
        WWW("www"),
        /**
         * Serves static resource like css, js, for modal dialogs on
         * delete-operations and similar things.
         */
        STATIC("static"),
        /**
         * Serves the same content as {@link #WWW}, but requires
         * authentification via client certificate.
         */
        SECURE("secure"),
        /**
         * Serves the API for issuing certificates, receiving Quiz results.
         */
        API("api"),
        /**
         * Hosts a link-redirector (not served by Gigi) for external links from
         * Gigi.
         */
        LINK("link"),
        /**
         * Hosts the certificate repository for the certificates generated
         * during NRE. Also not served by Gigi.
         */
        CRT_REPO("g2.crt");

        private final String value;

        private Host(String value) {
            this.value = value;
        }

        public String getConfigName() {
            return value;
        }

        public String getHostDefaultPrefix() {
            return value;
        }
    }

    private static Map<Host, String> hostnames;

    private static String securePort, port, secureBindPort, bindPort;

    private static String suffix = "wpia.local";

    private static String appName = null;

    private static String appIdentifier = null;

    public static void init(Properties conf) {
        securePort = port = "";
        if ( !conf.getProperty("https.port").equals("443")) {
            securePort = ":" + conf.getProperty("https.port");
        }
        if ( !conf.getProperty("http.port").equals("80")) {
            port = ":" + conf.getProperty("http.port");
        }
        secureBindPort = conf.getProperty("https.bindPort", conf.getProperty("https.port"));
        bindPort = conf.getProperty("http.bindPort", conf.getProperty("http.port"));

        suffix = conf.getProperty("name.suffix", "wpia.local");
        HashMap<Host, String> hostnames = new HashMap<>();
        for (Host h : Host.values()) {
            hostnames.put(h, conf.getProperty("name." + h.getConfigName(), h.getHostDefaultPrefix() + "." + suffix));
        }
        ServerConstants.hostnames = Collections.unmodifiableMap(hostnames);
        appName = conf.getProperty("appName");
        if (appName == null) {
            throw new Error("App name missing");
        }
        appIdentifier = conf.getProperty("appIdentifier");
        if (appIdentifier == null) {
            throw new Error("App identifier missing");
        }
    }

    public static String getHostName(Host h) {
        return hostnames.get(h);
    }

    public static String getHostNamePortSecure(Host h) {
        return hostnames.get(h) + securePort;
    }

    public static String getHostNamePort(Host h) {
        return hostnames.get(h) + port;
    }

    public static int getSecurePort() {
        if (secureBindPort != null && !secureBindPort.isEmpty()) {
            if (secureBindPort.equals("stdin")) {
                return -1;
            } else {
                return Integer.parseInt(secureBindPort);
            }
        }
        if (securePort.isEmpty()) {
            return 443;
        }
        return Integer.parseInt(securePort.substring(1, securePort.length()));
    }

    public static int getPort() {
        if (bindPort != null && !bindPort.isEmpty()) {
            if (bindPort.equals("stdin")) {
                return -1;
            } else {
                return Integer.parseInt(bindPort);
            }
        }
        if (port.isEmpty()) {
            return 80;
        }
        return Integer.parseInt(port.substring(1, port.length()));
    }

    public static String getSuffix() {
        return suffix;
    }

    public static String getSupportMailAddress() {
        return "support@" + getSuffix();
    }

    public static String getBoardMailAddress() {
        return "board@" + getSuffix();
    }

    public static String getQuizMailAddress() {
        return "quiz@" + getSuffix();
    }

    public static String getQuizAdminMailAddress() {
        return "quiz-admin@" + getSuffix();
    }

    public static String getAppName() {
        if (appName == null) {
            throw new Error("AppName not initialized.");
        }
        return appName;
    }

    public static String getAppIdentifier() {
        if (appIdentifier == null) {
            throw new Error("AppIdentifier not initialized.");
        }
        return appIdentifier;
    }

}
