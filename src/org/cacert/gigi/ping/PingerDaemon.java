package org.cacert.gigi.ping;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.DatabaseConnection.Link;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.dbObjects.DomainPingType;
import org.cacert.gigi.util.RandomToken;

public class PingerDaemon extends Thread {

    HashMap<DomainPingType, DomainPinger> pingers = new HashMap<>();

    private GigiPreparedStatement searchNeededPings;

    private KeyStore truststore;

    private Queue<DomainPingConfiguration> toExecute = new LinkedList<>();

    public PingerDaemon(KeyStore truststore) {
        this.truststore = truststore;
    }

    @Override
    public void run() {
        try (Link l = DatabaseConnection.newLink(false)) {
            runWithConnection();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runWithConnection() {
        searchNeededPings = new GigiPreparedStatement("SELECT `pc`.`id`" //
                + " FROM `pingconfig` AS `pc`" //
                + " INNER JOIN `domains` AS `d` ON `d`.`id` = `pc`.`domainid`" //
                + " WHERE `d`.`deleted` IS NULL" //
                + "  AND `pc`.`deleted` IS NULL" //
                + "  AND NOT EXISTS (" //
                + "    SELECT 1 FROM `domainPinglog` AS `dpl`" //
                + "    WHERE `dpl`.`configId` = `pc`.`id`" //
                + "     AND `dpl`.`when` >= CURRENT_TIMESTAMP - interval '6 mons')");
        pingers.put(DomainPingType.EMAIL, new EmailPinger());
        pingers.put(DomainPingType.SSL, new SSLPinger(truststore));
        pingers.put(DomainPingType.HTTP, new HTTPFetch());
        pingers.put(DomainPingType.DNS, new DNSPinger());

        while (true) {
            try {
                boolean worked = false;
                synchronized (this) {
                    DomainPingConfiguration conf;
                    while ((conf = toExecute.peek()) != null) {
                        worked = true;
                        handle(conf);
                        toExecute.remove();
                    }
                    notifyAll();
                }

                GigiResultSet rs = searchNeededPings.executeQuery();
                while (rs.next()) {
                    worked = true;
                    handle(DomainPingConfiguration.getById(rs.getInt("id")));
                }
                try {
                    if ( !worked) {
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void handle(DomainPingConfiguration conf) {
        DomainPingType type = conf.getType();
        String config = conf.getInfo();
        DomainPinger dp = pingers.get(type);
        if (dp != null) {
            if (dp instanceof EmailPinger) {
                String token = null;
                token = RandomToken.generateToken(16);
                config = config + ":" + token;
            }
            Domain target = conf.getTarget();
            System.err.println("Executing " + dp + " on " + target + " (" + System.currentTimeMillis() + ")");
            try {
                dp.ping(target, config, target.getOwner(), conf.getId());
            } catch (Throwable t) {
                t.printStackTrace();
                DomainPinger.enterPingResult(conf.getId(), "error", "exception", null);
            }
            System.err.println("done (" + System.currentTimeMillis() + ")");
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
