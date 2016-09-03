package org.cacert.gigi.ping;

import org.cacert.gigi.database.DBEnum;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;

public abstract class DomainPinger {

    public static enum PingState implements DBEnum {
        OPEN, SUCCESS, FAILED;

        @Override
        public String getDBName() {
            return toString().toLowerCase();
        }
    }

    public static final String PING_STILL_PENDING = null;

    public static final String PING_SUCCEDED = "";

    public abstract void ping(Domain domain, String configuration, CertificateOwner target, int confId);

    protected static void enterPingResult(int configId, String state, String result, String token) {
        PingState estate = DomainPinger.PING_STILL_PENDING == state ? PingState.OPEN : DomainPinger.PING_SUCCEDED.equals(state) ? PingState.SUCCESS : PingState.FAILED;
        try (GigiPreparedStatement enterPingResult = new GigiPreparedStatement("INSERT INTO `domainPinglog` SET `configId`=?, `state`=?::`pingState`, `result`=?, `challenge`=?")) {
            enterPingResult.setInt(1, configId);
            enterPingResult.setEnum(2, estate);
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
