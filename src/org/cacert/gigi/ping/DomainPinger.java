package org.cacert.gigi.ping;

public abstract class DomainPinger {

    public abstract void ping(String domain, String configuration, String token);
}
