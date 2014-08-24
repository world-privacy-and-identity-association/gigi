package org.cacert.gigi.ping;

import org.cacert.gigi.Domain;
import org.cacert.gigi.User;

public abstract class DomainPinger {

    public static final String PING_STILL_PENDING = null;

    public static final String PING_SUCCEDED = "";

    public abstract String ping(Domain domain, String configuration, User user);
}
