package org.cacert.gigi.dbObjects;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.util.DomainAssessment;

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
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `domains` WHERE (`domain`=? OR (CONCAT('.', `domain`)=RIGHT(?,LENGTH(`domain`)+1)  OR RIGHT(`domain`,LENGTH(?)+1)=CONCAT('.',?))) AND `deleted` IS NULL")) {
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

    public List<DomainPingConfiguration> getConfiguredPings() throws GigiApiException {
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
            ps.setString(2, type.toString().toLowerCase());
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

    public synchronized void verify(String hash) throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `domainPinglog` SET `state`='success' WHERE `challenge`=? AND `state`='open' AND `configId` IN (SELECT `id` FROM `pingconfig` WHERE `domainid`=? AND `type`='email')")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public boolean isVerified() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `domainPinglog` INNER JOIN `pingconfig` ON `pingconfig`.`id`=`domainPinglog`.`configId` WHERE `domainid`=? AND `state`='success'")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            return rs.next();
        }
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

    public static Domain searchUserIdByDomain(String domain) {
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

}
