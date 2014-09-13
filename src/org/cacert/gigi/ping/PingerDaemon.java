package org.cacert.gigi.ping;

import java.security.KeyStore;
import java.util.HashMap;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.util.RandomToken;

public class PingerDaemon extends Thread {

    HashMap<String, DomainPinger> pingers = new HashMap<>();

    private GigiPreparedStatement searchNeededPings;

    private GigiPreparedStatement enterPingResult;

    private GigiPreparedStatement updatePingStatus;

    private KeyStore truststore;

    public PingerDaemon(KeyStore truststore) {
        this.truststore = truststore;
    }

    @Override
    public void run() {
        searchNeededPings = DatabaseConnection.getInstance().prepare("SELECT pingconfig.*, domains.domain, domains.memid FROM pingconfig LEFT JOIN domainPinglog ON domainPinglog.configId=pingconfig.id INNER JOIN domains ON domains.id=pingconfig.domainid WHERE ( pingconfig.reping='y' OR domainPinglog.configId IS NULL) AND domains.deleted IS NULL GROUP BY pingconfig.id");
        enterPingResult = DatabaseConnection.getInstance().prepare("INSERT INTO domainPinglog SET configId=?, state=?, result=?, challenge=?");
        updatePingStatus = DatabaseConnection.getInstance().prepare("UPDATE pingconfig SET reping='n' WHERE id=?");
        pingers.put("email", new EmailPinger());
        pingers.put("ssl", new SSLPinger(truststore));
        pingers.put("http", new HTTPFetch());
        pingers.put("dns", new DNSPinger());

        while (true) {
            execute();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void execute() {

        GigiResultSet rs = searchNeededPings.executeQuery();
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
                updatePingStatus.setInt(1, rs.getInt("id"));
                updatePingStatus.execute();
                enterPingResult.setInt(1, rs.getInt("id"));
                String resp = dp.ping(Domain.getById(rs.getInt("domainid")), config, User.getById(rs.getInt("memid")));
                enterPingResult.setString(2, DomainPinger.PING_STILL_PENDING == resp ? "open" : DomainPinger.PING_SUCCEDED.equals(resp) ? "success" : "failed");
                enterPingResult.setString(3, resp);
                enterPingResult.setString(4, token);
                enterPingResult.execute();
            }
        }
    }

}
