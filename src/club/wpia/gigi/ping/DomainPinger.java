package club.wpia.gigi.ping;

import club.wpia.gigi.database.DBEnum;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingExecution;

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

    public abstract DomainPingExecution ping(Domain domain, String configuration, CertificateOwner target, DomainPingConfiguration conf);

    protected static DomainPingExecution enterPingResult(DomainPingConfiguration config, String state, String result, String token) {
        PingState estate = DomainPinger.PING_STILL_PENDING == state ? PingState.OPEN : DomainPinger.PING_SUCCEDED.equals(state) ? PingState.SUCCESS : PingState.FAILED;
        return new DomainPingExecution(estate, result, config, token);
    }

    protected static void updatePingResult(DomainPingConfiguration config, String state, String result, String token) {
        try (GigiPreparedStatement updatePingResult = new GigiPreparedStatement("UPDATE `domainPinglog` SET `state`=?::`pingState`, `result`=? WHERE `configId`=? AND `challenge`=?")) {
            updatePingResult.setString(1, DomainPinger.PING_STILL_PENDING == state ? "open" : DomainPinger.PING_SUCCEDED.equals(state) ? "success" : "failed");
            updatePingResult.setString(2, result);
            updatePingResult.setInt(3, config.getId());
            updatePingResult.setString(4, token);
            updatePingResult.execute();
        }
    }

}
