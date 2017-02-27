package club.wpia.gigi.util;

import club.wpia.gigi.util.ServerConstants.Host;

public class SystemKeywords {

    public static final String CAA_NAME = ServerConstants.getSuffix();

    public static final String SMTP_NAME = ServerConstants.getHostName(Host.WWW);

    public static final String SMTP_PSEUDO_FROM = "returns@" + ServerConstants.getSuffix();

    public static final String HTTP_CHALLENGE_PREFIX = ".well-known/" + ServerConstants.getAppIdentifier() + "-challenge/";

    public static final String DNS_PREFIX = "_" + ServerConstants.getAppIdentifier();
}
