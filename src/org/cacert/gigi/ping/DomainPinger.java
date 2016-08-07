package org.cacert.gigi.ping;

import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;

public abstract class DomainPinger {

    public static final String PING_STILL_PENDING = null;

    public static final String PING_SUCCEDED = "";

    public abstract void ping(Domain domain, String configuration, CertificateOwner target, int confId);

    protected static void enterPingResult(int configId, String state, String result, String token) {
        try (GigiPreparedStatement enterPingResult = new GigiPreparedStatement("INSERT INTO `domainPinglog` SET `configId`=?, `state`=?::`pingState`, `result`=?, `challenge`=?")) {
            enterPingResult.setInt(1, configId);
            enterPingResult.setString(2, DomainPinger.PING_STILL_PENDING == state ? "open" : DomainPinger.PING_SUCCEDED.equals(state) ? "success" : "failed");
            enterPingResult.setString(3, result);
            enterPingResult.setString(4, token);
            enterPingResult.execute();
        }
    }

    protected static void updatePingResult(int configId, String state, String result, String token) {
        try (GigiPreparedStatement updatePingResult = new GigiPreparedStatement("UPDATE `domainPinglog` SET `state`=?::`pingState`, `result`=? WHERE `configId`=? AND `challenge`=?")) {
            updatePingResult.setString(1, DomainPinger.PING_STILL_PENDING == state ? "open" : DomainPinger.PING_SUCCEDED.equals(state) ? "success" : "failed");
            updatePingResult.setString(2, result);
            updatePingResult.setInt(3, configId);
            updatePingResult.setString(4, token);
            updatePingResult.execute();
        }
    }

}
