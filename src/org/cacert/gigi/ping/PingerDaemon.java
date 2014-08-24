package org.cacert.gigi.ping;

import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.cacert.gigi.Domain;
import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.RandomToken;

public class PingerDaemon extends Thread {

    HashMap<String, DomainPinger> pingers = new HashMap<>();

    private PreparedStatement searchNeededPings;

    private PreparedStatement enterPingResult;

    @Override
    public void run() {
        try {
            searchNeededPings = DatabaseConnection.getInstance().prepare("SELECT pingconfig.*, domains.domain, domains.memid FROM pingconfig LEFT JOIN domainPinglog ON domainPinglog.configId=pingconfig.id INNER JOIN domains ON domains.id=pingconfig.domainid WHERE domainPinglog.configId IS NULL ");
            enterPingResult = DatabaseConnection.getInstance().prepare("INSERT INTO domainPinglog SET configId=?, state=?, result=?, challenge=?");
            pingers.put("email", new EmailPinger());
            pingers.put("ssl", new SSLPinger());
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
                e.printStackTrace();
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
                enterPingResult.setString(2, resp == DomainPinger.PING_STILL_PENDING ? "open" : resp == DomainPinger.PING_SUCCEDED ? "success" : "failed");
                enterPingResult.setString(3, resp);
                enterPingResult.setString(4, token);
                enterPingResult.execute();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Properties conf = new Properties();
        conf.load(new FileReader("config/gigi.properties"));
        DatabaseConnection.init(conf);
        new PingerDaemon().run();

    }
}
