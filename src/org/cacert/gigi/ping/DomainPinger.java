package org.cacert.gigi.ping;

public abstract class DomainPinger {

    public static final String PING_STILL_PENDING = null;

    public static final String PING_SUCCEDED = "";

    public abstract String ping(String domain, String configuration);
}
