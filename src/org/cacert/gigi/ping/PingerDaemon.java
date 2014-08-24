package org.cacert.gigi.ping;

import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.cacert.gigi.database.DatabaseConnection;

public class PingerDaemon implements Runnable {

    HashMap<String, DomainPinger> pingers = new HashMap<>();

    public PingerDaemon() {
        // pingers.put("email",);
        pingers.put("ssl", new SSLPinger());
        pingers.put("http", new HTTPFetch());
        pingers.put("dns", new DNSPinger());

    }

    @Override
    public void run() {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT pingconfig.*, domains.domain FROM pingconfig LEFT JOIN domainPinglog ON domainPinglog.configId=pingconfig.id INNER JOIN domains ON domains.id=pingconfig.domainid WHERE domainPinglog.configId IS NULL ");
            PreparedStatement result = DatabaseConnection.getInstance().prepare("INSERT INTO domainPinglog SET configId=?, state=?, result=?");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("type");
                String config = rs.getString("info");
                System.out.println(type);
                System.out.println(config);
                DomainPinger dp = pingers.get(type);
                if (dp != null) {
                    result.setInt(1, rs.getInt("id"));
                    String resp = dp.ping(rs.getString("domain"), config);
                    result.setString(2, resp == DomainPinger.PING_STILL_PENDING ? "open" : resp == DomainPinger.PING_SUCCEDED ? "success" : "failed");
                    result.setString(3, resp);
                    result.execute();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Properties conf = new Properties();
        conf.load(new FileReader("config/gigi.properties"));
        DatabaseConnection.init(conf);
        new PingerDaemon().run();

    }
}
