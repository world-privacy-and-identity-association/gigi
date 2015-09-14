package org.cacert.gigi.ping;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.dbObjects.DomainPingConfiguration.PingType;
import org.cacert.gigi.util.RandomToken;

public class PingerDaemon extends Thread {

    HashMap<PingType, DomainPinger> pingers = new HashMap<>();

    private GigiPreparedStatement searchNeededPings;

    private GigiPreparedStatement enterPingResult;

    private KeyStore truststore;

    private Queue<DomainPingConfiguration> toExecute = new LinkedList<>();

    public PingerDaemon(KeyStore truststore) {
        this.truststore = truststore;
    }

    @Override
    public void run() {
        searchNeededPings = DatabaseConnection.getInstance().prepare("SELECT `pingconfig`.`id` FROM `pingconfig` LEFT JOIN `domainPinglog` ON `domainPinglog`.`configId` = `pingconfig`.`id` INNER JOIN `domains` ON `domains`.`id` = `pingconfig`.`domainid` WHERE ( `domainPinglog`.`configId` IS NULL) AND `domains`.`deleted` IS NULL GROUP BY `pingconfig`.`id`");
        enterPingResult = DatabaseConnection.getInstance().prepare("INSERT INTO `domainPinglog` SET `configId`=?, `state`=?, `result`=?, `challenge`=?");
        pingers.put(PingType.EMAIL, new EmailPinger());
        pingers.put(PingType.SSL, new SSLPinger(truststore));
        pingers.put(PingType.HTTP, new HTTPFetch());
        pingers.put(PingType.DNS, new DNSPinger());

        while (true) {
            synchronized (this) {
                DomainPingConfiguration conf;
                while ((conf = toExecute.peek()) != null) {
                    handle(conf);
                    toExecute.remove();
                }
                notifyAll();
            }

            GigiResultSet rs = searchNeededPings.executeQuery();
            while (rs.next()) {
                handle(DomainPingConfiguration.getById(rs.getInt("id")));
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void handle(DomainPingConfiguration conf) {
        PingType type = conf.getType();
        String config = conf.getInfo();
        DomainPinger dp = pingers.get(type);
        if (dp != null) {
            String token = null;
            if (dp instanceof EmailPinger) {
                token = RandomToken.generateToken(16);
                config = config + ":" + token;
            }
            enterPingResult.setInt(1, conf.getId());
            Domain target = conf.getTarget();
            String resp = dp.ping(target, config, target.getOwner());
            enterPingResult.setString(2, DomainPinger.PING_STILL_PENDING == resp ? "open" : DomainPinger.PING_SUCCEDED.equals(resp) ? "success" : "failed");
            enterPingResult.setString(3, resp);
            enterPingResult.setString(4, token);
            enterPingResult.execute();
        }
    }

    public synchronized void queue(DomainPingConfiguration toReping) {
        interrupt();
        toExecute.add(toReping);
        while (toExecute.size() > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
