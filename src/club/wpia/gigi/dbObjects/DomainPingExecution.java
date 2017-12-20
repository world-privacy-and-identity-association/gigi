package club.wpia.gigi.dbObjects;

import java.sql.Timestamp;
import java.util.Date;

import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.ping.DomainPinger.PingState;

public class DomainPingExecution {

    private final PingState state;

    private final String type;

    private final String info;

    private final String result;

    private final DomainPingConfiguration config;

    private final Timestamp date;

    protected DomainPingExecution(GigiResultSet rs) {
        state = PingState.valueOf(rs.getString(1).toUpperCase());
        type = rs.getString(2);
        info = rs.getString(3);
        result = rs.getString(4);
        config = DomainPingConfiguration.getById(rs.getInt(5));
        date = rs.getTimestamp(6);
    }

    public DomainPingExecution(PingState state, String result, DomainPingConfiguration config, String challenge) {
        this.state = state;
        this.type = config.getType().getDBName();
        this.info = config.getInfo();
        this.result = result;
        this.config = config;
        this.date = new Timestamp(System.currentTimeMillis());
        try (GigiPreparedStatement enterPingResult = new GigiPreparedStatement("INSERT INTO `domainPinglog` SET `configId`=?, `state`=?::`pingState`, `result`=?, `challenge`=?, `when`=?, `needsAction`=?")) {
            enterPingResult.setInt(1, config.getId());
            enterPingResult.setEnum(2, state);
            enterPingResult.setString(3, result);
            enterPingResult.setString(4, challenge);
            enterPingResult.setTimestamp(5, this.date);
            // Ping results with current state "failed" need followup action in
            // two weeks to revoke any remaining active certificates.
            enterPingResult.setBoolean(6, state == PingState.FAILED);
            enterPingResult.execute();
        }
    }

    public PingState getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public String getInfo() {
        return info;
    }

    public String getResult() {
        return result;
    }

    public DomainPingConfiguration getConfig() {
        return config;
    }

    public Date getDate() {
        return date;
    }

}
