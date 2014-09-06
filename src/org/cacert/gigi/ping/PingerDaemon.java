package org.cacert.gigi.ping;

import java.security.KeyStore;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.util.RandomToken;

public class PingerDaemon extends Thread {

    HashMap<String, DomainPinger> pingers = new HashMap<>();

    private PreparedStatement searchNeededPings;

    private PreparedStatement enterPingResult;

    private KeyStore truststore;

    public PingerDaemon(KeyStore truststore) {
        this.truststore = truststore;
    }

    @Override
    public void run() {
        try {
            searchNeededPings = DatabaseConnection.getInstance().prepare("SELECT pingconfig.*, domains.domain, domains.memid FROM pingconfig LEFT JOIN domainPinglog ON domainPinglog.configId=pingconfig.id INNER JOIN domains ON domains.id=pingconfig.domainid WHERE domainPinglog.configId IS NULL AND domains.deleted IS NULL ");
            enterPingResult = DatabaseConnection.getInstance().prepare("INSERT INTO domainPinglog SET configId=?, state=?, result=?, challenge=?");
            pingers.put("email", new EmailPinger());
            pingers.put("ssl", new SSLPinger(truststore));
            pingers.put("http", new HTTPFetch());
            pingers.put("dns", new DNSPinger());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void execute() throws SQLException {

        ResultSet rs = searchNeededPings.executeQuery();
        while (rs.next()) {
            String type = rs.getString("type");
            String config = rs.getString("info");
            DomainPinger dp = pingers.get(type);
            if (dp != null) {
                String token = null;
                if (dp instanceof EmailPinger) {
                    token = RandomToken.generateToken(16);
                    config = config + ":" + token;
                }
                enterPingResult.setInt(1, rs.getInt("id"));
                String resp = dp.ping(Domain.getById(rs.getInt("domainid")), config, User.getById(rs.getInt("memid")));
                enterPingResult.setString(2, DomainPinger.PING_STILL_PENDING == resp ? "open" : DomainPinger.PING_SUCCEDED.equals(resp) ? "success" : "failed");
                enterPingResult.setString(3, resp);
                enterPingResult.setString(4, token);
                enterPingResult.execute();
            }
        }
    }

    public void requestReping(DomainPingConfiguration dpc) {}
}
