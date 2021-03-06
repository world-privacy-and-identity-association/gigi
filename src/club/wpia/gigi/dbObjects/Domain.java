package club.wpia.gigi.dbObjects;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.Certificate.RevocationType;
import club.wpia.gigi.util.DomainAssessment;

public class Domain implements IdCachable, Verifyable {

    private CertificateOwner owner;

    private String suffix;

    private int id;

    private Domain(GigiResultSet rs, int id) {
        this.id = id;
        owner = CertificateOwner.getById(rs.getInt(1));
        suffix = rs.getString(2);
    }

    public Domain(User actor, CertificateOwner owner, String suffix) throws GigiApiException {
        suffix = suffix.toLowerCase();
        synchronized (Domain.class) {
            DomainAssessment.checkCertifiableDomain(suffix, actor.isInGroup(Group.CODESIGNING), true);
            this.owner = owner;
            this.suffix = suffix;
            insert();
        }
    }

    private static void checkInsert(String suffix) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `domains` WHERE (`domain`=? OR (CONCAT('.', `domain`)=RIGHT(?,LENGTH(`domain`)+1)  OR RIGHT(`domain`,LENGTH(?)+1)=CONCAT('.',?::VARCHAR))) AND `deleted` IS NULL")) {
            ps.setString(1, suffix);
            ps.setString(2, suffix);
            ps.setString(3, suffix);
            ps.setString(4, suffix);
            GigiResultSet rs = ps.executeQuery();
            boolean existed = rs.next();
            rs.close();
            if (existed) {
                throw new GigiApiException("Domain could not be inserted. Domain is already known to the system.");
            }
        }
    }

    private void insert() throws GigiApiException {
        if (id != 0) {
            throw new GigiApiException("already inserted.");
        }
        checkInsert(suffix);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `domains` SET memid=?, domain=?")) {
            ps.setInt(1, owner.getId());
            ps.setString(2, suffix);
            ps.execute();
            id = ps.lastInsertId();
        }
        myCache.put(this);
    }

    public void delete() throws GigiApiException {
        if (id == 0) {
            throw new GigiApiException("not inserted.");
        }
        synchronized (Domain.class) {
            myCache.remove(this);
            try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `domains` SET `deleted`=CURRENT_TIMESTAMP WHERE `id`=?")) {
                ps.setInt(1, id);
                ps.execute();
            }
            LinkedList<Job> revokes = new LinkedList<Job>();
            for (Certificate cert : fetchActiveCertificates()) {
                revokes.add(cert.revoke(RevocationType.USER));
            }
            long start = System.currentTimeMillis();
            for (Job job : revokes) {
                int toWait = (int) (60000 + start - System.currentTimeMillis());
                if (toWait > 0) {
                    job.waitFor(toWait);
                } else {
                    break; // canceled... waited too log
                }
            }
        }
    }

    public CertificateOwner getOwner() {
        return owner;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getSuffix() {
        return suffix;
    }

    private LinkedList<DomainPingConfiguration> configs = null;

    public List<DomainPingConfiguration> getConfiguredPings() {
        LinkedList<DomainPingConfiguration> configs = this.configs;
        if (configs == null) {
            configs = new LinkedList<>();
            try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT id FROM pingconfig WHERE domainid=? AND `deleted` IS NULL")) {
                ps.setInt(1, id);
                GigiResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    configs.add(DomainPingConfiguration.getById(rs.getInt(1)));
                }
            }
            this.configs = configs;

        }
        return Collections.unmodifiableList(configs);
    }

    public void addPing(DomainPingType type, String config) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `pingconfig` SET `domainid`=?, `type`=?::`pingType`, `info`=?")) {
            ps.setInt(1, id);
            ps.setEnum(2, type);
            ps.setString(3, config);
            ps.execute();
        }
        configs = null;
    }

    public void clearPings() throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `pingconfig` SET `deleted`=CURRENT_TIMESTAMP WHERE `deleted` is NULL AND `domainid`=?")) {
            ps.setInt(1, id);
            ps.execute();
        }
        configs = null;
    }

    public synchronized boolean isVerifyable(String hash) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `domainPinglog` WHERE `challenge`=? AND `state`='open' AND `configId` IN (SELECT `id` FROM `pingconfig` WHERE `domainid`=? AND `type`='email')")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            return ps.executeQuery().next();
        }
    }

    public synchronized void verify(String hash) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `domainPinglog` SET `state`='success' WHERE `challenge`=? AND `state`='open' AND `configId` IN (SELECT `id` FROM `pingconfig` WHERE `domainid`=? AND `type`='email')")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            if ( !ps.executeMaybeUpdate()) {
                throw new IllegalArgumentException("Given token could not be found to complete the verification process (Domain Ping).");
            }
        }
    }

    /**
     * Determines current domain validity. A domain is valid, iff at least two
     * configured pings are currently successful.
     * 
     * @return true, iff domain is valid
     * @throws GigiApiException
     */
    public boolean isVerified() {
        int count = 0;
        boolean[] used = new boolean[DomainPingType.values().length];
        for (DomainPingConfiguration config : getConfiguredPings()) {
            if (config.isValid() && !used[config.getType().ordinal()]) {
                count++;
                used[config.getType().ordinal()] = true;
            }
            if (count >= 2) {
                return true;
            }
        }
        return false;
    }

    public DomainPingExecution[] getPings() throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `state`, `type`, `info`, `result`, `configId`, `when` FROM `domainPinglog` INNER JOIN `pingconfig` ON `pingconfig`.`id`=`domainPinglog`.`configId` WHERE `pingconfig`.`domainid`=? ORDER BY `when` DESC;", true)) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            rs.last();
            DomainPingExecution[] contents = new DomainPingExecution[rs.getRow()];
            rs.beforeFirst();
            for (int i = 0; i < contents.length && rs.next(); i++) {
                contents[i] = new DomainPingExecution(rs);
            }
            return contents;
        }

    }

    private static final ObjectCache<Domain> myCache = new ObjectCache<>();

    public static synchronized Domain getById(int id) {
        Domain em = myCache.get(id);
        if (em == null) {
            try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `memid`, `domain` FROM `domains` WHERE `id`=? AND `deleted` IS NULL")) {
                ps.setInt(1, id);
                GigiResultSet rs = ps.executeQuery();
                if ( !rs.next()) {
                    return null;
                }
                myCache.put(em = new Domain(rs, id));
            }
        }
        return em;
    }

    public static Domain searchDomain(String domain) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id` FROM `domains` WHERE `domain` = ? AND `deleted` IS NULL")) {
            ps.setString(1, domain);
            GigiResultSet res = ps.executeQuery();
            if (res.next()) {
                return getById(res.getInt(1));
            } else {
                return null;
            }
        }
    }

    public Certificate[] fetchActiveCertificates() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `certs`.`id` FROM `certs` INNER JOIN `subjectAlternativeNames` ON `subjectAlternativeNames`.`certId` = `certs`.`id` WHERE (`contents`=? OR RIGHT(`contents`,LENGTH(?)+1)=CONCAT('.',?::VARCHAR)) AND `type`='DNS' AND `revoked` IS NULL AND `expire` > CURRENT_TIMESTAMP AND `memid`=? GROUP BY `certs`.`id`", true)) {
            ps.setString(1, suffix);
            ps.setString(2, suffix);
            ps.setString(3, suffix);
            ps.setInt(4, owner.getId());
            GigiResultSet rs = ps.executeQuery();
            rs.last();
            Certificate[] res = new Certificate[rs.getRow()];
            rs.beforeFirst();
            int i = 0;
            while (rs.next()) {
                res[i++] = Certificate.getById(rs.getInt(1));
            }
            return res;
        }
    }

}
