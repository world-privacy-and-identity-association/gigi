package club.wpia.gigi.util;

import java.util.Properties;

public class ServerConstants {

    private static String wwwHostName = "www.wpia.local";

    private static String secureHostName = "secure.wpia.local";

    private static String staticHostName = "static.wpia.local";

    private static String apiHostName = "api.wpia.local";

    private static String securePort, port, secureBindPort, bindPort;

    private static String suffix = "wpia.local";

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
        wwwHostName = conf.getProperty("name.www");
        secureHostName = conf.getProperty("name.secure");
        staticHostName = conf.getProperty("name.static");
        apiHostName = conf.getProperty("name.api");
        suffix = conf.getProperty("name.suffix", conf.getProperty("name.www").substring(4));

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

    public static String getSecureHostNamePortSecure() {
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
        return "support@" + ServerConstants.getWwwHostName().replaceFirst("^www\\.", "");
    }

    public static String getBoardMailAddress() {
        return "board@" + ServerConstants.getWwwHostName().replaceFirst("^www\\.", "");
    }

    public static String getQuizMailAddress() {
        return "quiz@" + ServerConstants.getWwwHostName().replaceFirst("^www\\.", "");
    }

    public static String getQuizAdminMailAddress() {
        return "quiz-admin@" + ServerConstants.getWwwHostName().replaceFirst("^www\\.", "");
    }

}
