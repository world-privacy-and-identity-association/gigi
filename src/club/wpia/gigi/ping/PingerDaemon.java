package club.wpia.gigi.ping;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.DatabaseConnection.Link;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.RevocationType;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingExecution;
import club.wpia.gigi.dbObjects.DomainPingType;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.ArrayIterable;
import club.wpia.gigi.output.template.MailTemplate;
import club.wpia.gigi.pages.account.domain.EditDomain;
import club.wpia.gigi.ping.DomainPinger.PingState;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class PingerDaemon extends Thread {

    HashMap<DomainPingType, DomainPinger> pingers = new HashMap<>();

    private KeyStore truststore;

    private Queue<DomainPingConfiguration> toExecute = new LinkedList<>();

    private final MailTemplate pingFailedMail = new MailTemplate(PingerDaemon.class.getResource("PingFailedWithActiveCertificates.templ"));

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

        initializeConnectionUsage();

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
                long time = System.currentTimeMillis();
                worked |= executeNeededPings(new Date(time));
                try {
                    if ( !worked) {
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                }
            } catch (Throwable t) {
                t.printStackTrace();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    protected void initializeConnectionUsage() {
        pingers.put(DomainPingType.EMAIL, new EmailPinger());
        pingers.put(DomainPingType.SSL, new SSLPinger(truststore));
        pingers.put(DomainPingType.HTTP, new HTTPFetch());
        pingers.put(DomainPingType.DNS, new DNSPinger());
    }

    public synchronized boolean executeNeededPings(Date time) {
        boolean worked = false;
        try (GigiPreparedStatement searchNeededPings = new GigiPreparedStatement("SELECT `d`.`id`, `dpl`.`configId`, `dpl`.`when`," //
        // .. for all found pings we want to know, if we do not have a more
        // recent successful ping
                + "  NOT EXISTS (" //
                + "    SELECT 1 FROM `domainPinglog` AS `dpl2`" //
                + "    WHERE `dpl`.`configId` = `dpl2`.`configId`" //
                + "     AND `dpl2`.state = 'success' AND `dpl2`.`when` > `dpl`.`when`) AS `resucceeded`" //
                + " FROM `domainPinglog` AS `dpl`" //
                // We search valid pings
                + " INNER JOIN `pingconfig` AS `pc` ON `pc`.`id` = `dpl`.`configId` AND `pc`.`deleted` IS NULL" //
                + " INNER JOIN `domains` AS `d` ON `d`.`id` = `pc`.`domainid` AND `d`.`deleted` IS NULL" //
                // .. that failed, ..
                + " WHERE `dpl`.`state` = 'failed'" //
                // .. are older than 2 weeks
                + " AND `dpl`.`when` <= ?::timestamp - interval '2 weeks'" //
                // .. and are flagged for corrective action
                + " AND `dpl`.`needsAction`" //
        )) {
            searchNeededPings.setTimestamp(1, new Timestamp(time.getTime()));
            GigiResultSet rs = searchNeededPings.executeQuery();
            try (GigiPreparedStatement updateDone = new GigiPreparedStatement("UPDATE `domainPinglog` SET `needsAction`=false WHERE `configId`=? AND `when`=?")) {
                while (rs.next()) {
                    worked = true;
                    // Give this ping a last chance to succeed.
                    handle(DomainPingConfiguration.getById(rs.getInt(2)));
                    // We only consider revoking if this ping has not been
                    // superseded by a following successful ping.
                    if (rs.getBoolean(4)) {
                        Domain d = Domain.getById(rs.getInt(1));
                        int ct = 0;
                        boolean[] used = new boolean[DomainPingType.values().length];
                        // We only revoke, there are not 2 pings that are not
                        // 'strictly invalid'
                        for (DomainPingConfiguration cfg : d.getConfiguredPings()) {
                            if ( !cfg.isStrictlyInvalid(time) && !used[cfg.getType().ordinal()]) {
                                ct++;
                                used[cfg.getType().ordinal()] = true;
                            }
                            if (ct >= 2) {
                                break;
                            }
                        }
                        if (ct < 2) {
                            for (Certificate c : d.fetchActiveCertificates()) {
                                // TODO notify user
                                c.revoke(RevocationType.PING_TIMEOUT);
                            }
                        }
                    }
                    updateDone.setInt(1, rs.getInt(2));
                    updateDone.setTimestamp(2, rs.getTimestamp(3));
                    updateDone.executeUpdate();
                }
            }
        }
        try (GigiPreparedStatement searchNeededPings = new GigiPreparedStatement("SELECT `pc`.`id`" //
                + " FROM `pingconfig` AS `pc`" //
                + " INNER JOIN `domains` AS `d` ON `d`.`id` = `pc`.`domainid`" //
                + " WHERE `d`.`deleted` IS NULL" //
                + "  AND `pc`.`deleted` IS NULL" //
                + "  AND NOT EXISTS (" //
                + "    SELECT 1 FROM `domainPinglog` AS `dpl`" //
                + "    WHERE `dpl`.`configId` = `pc`.`id`" //
                + "     AND `dpl`.`when` >= ?::timestamp - interval '6 mons')")) {
            searchNeededPings.setTimestamp(1, new Timestamp(time.getTime()));
            GigiResultSet rs = searchNeededPings.executeQuery();
            while (rs.next()) {
                worked = true;
                handle(DomainPingConfiguration.getById(rs.getInt("id")));
            }
        }
        return worked;
    }

    protected void handle(DomainPingConfiguration conf) {
        DomainPingType type = conf.getType();
        String config = conf.getInfo();
        DomainPinger dp = pingers.get(type);
        if (dp != null) {
            Domain target = conf.getTarget();
            System.err.println("Executing " + dp + " on " + target + " (" + System.currentTimeMillis() + ")");
            try {
                DomainPingExecution x = dp.ping(target, config, target.getOwner(), conf);
                if (x.getState() == PingState.FAILED) {
                    Certificate[] cs = target.fetchActiveCertificates();
                    if (cs.length != 0) {
                        CertificateOwner o = target.getOwner();
                        Locale l = Locale.ENGLISH;
                        String contact;
                        if (o instanceof User) {
                            l = ((User) o).getPreferredLocale();
                            contact = ((User) o).getEmail();
                        } else if (o instanceof Organisation) {
                            contact = ((Organisation) o).getContactEmail();

                        } else {
                            throw new Error();
                        }
                        HashMap<String, Object> vars = new HashMap<>();
                        vars.put("valid", target.isVerified());
                        vars.put("domain", target.getSuffix());
                        vars.put("domainLink", "https://" + ServerConstants.getHostNamePortSecure(Host.WWW) + "/" + EditDomain.PATH + target.getId());
                        vars.put("certs", new ArrayIterable<Certificate>(cs) {

                            @Override
                            public void apply(Certificate t, Language l, Map<String, Object> vars) {
                                vars.put("serial", t.getSerial());
                                vars.put("ca", t.getParent().getKeyname());
                                try {
                                    X509Certificate c = t.cert();
                                    vars.put("from", c.getNotBefore());
                                    vars.put("to", c.getNotAfter());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (GeneralSecurityException e) {
                                    e.printStackTrace();
                                } catch (GigiApiException e) {
                                    e.printStackTrace();
                                }
                            }

                        });
                        pingFailedMail.sendMail(Language.getInstance(l), vars, contact);
                        System.out.println("Ping failed with active certificates");
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                DomainPinger.enterPingResult(conf, "error", "exception", null);
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
