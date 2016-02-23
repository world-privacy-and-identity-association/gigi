package org.cacert.gigi.dbObjects;

import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.util.PublicSuffixes;

public class Domain implements IdCachable, Verifyable {

    private CertificateOwner owner;

    private String suffix;

    private int id;

    private static final Set<String> IDNEnabledTLDs;

    static {
        Properties CPS = new Properties();
        try (InputStream resourceAsStream = Domain.class.getResourceAsStream("CPS.properties")) {
            CPS.load(resourceAsStream);
            IDNEnabledTLDs = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(CPS.getProperty("IDN-enabled").split(","))));
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private Domain(int id) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `memid`, `domain` FROM `domains` WHERE `id`=? AND `deleted` IS NULL")) {
            ps.setInt(1, id);

            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                throw new IllegalArgumentException("Invalid domain id " + id);
            }
            this.id = id;
            owner = CertificateOwner.getById(rs.getInt(1));
            suffix = rs.getString(2);
        }
    }

    public Domain(User actor, CertificateOwner owner, String suffix) throws GigiApiException {
        synchronized (Domain.class) {
            checkCertifyableDomain(suffix, actor.isInGroup(Group.CODESIGNING));
            this.owner = owner;
            this.suffix = suffix;
            insert();
        }
    }

    public static void checkCertifyableDomain(String s, boolean hasPunycodeRight) throws GigiApiException {
        String[] parts = s.split("\\.", -1);
        if (parts.length < 2) {
            throw new GigiApiException("Domain does not contain '.'.");
        }
        for (int i = parts.length - 1; i >= 0; i--) {
            if ( !isVaildDomainPart(parts[i], hasPunycodeRight)) {
                throw new GigiApiException("Syntax error in Domain");
            }
        }
        String publicSuffix = PublicSuffixes.getInstance().getRegistrablePart(s);
        if ( !s.equals(publicSuffix)) {
            throw new GigiApiException("You may only register a domain with exactly one lable before the public suffix.");
        }
        if (("." + s).matches("(.[0-9]*)*")) {
            // This is not reached because we currently have no TLD that is
            // numbers only. But who knows..
            // Better safe than sorry.
            throw new GigiApiException("IP Addresses are not allowed");
        }
        checkPunycode(parts[0], s.substring(parts[0].length() + 1));
    }

    private static void checkPunycode(String label, String domainContext) throws GigiApiException {
        if (label.charAt(2) != '-' || label.charAt(3) != '-') {
            return; // is no punycode
        }
        if ( !IDNEnabledTLDs.contains(domainContext)) {
            throw new GigiApiException("Punycode label could not be positively verified.");
        }
        if ( !label.startsWith("xn--")) {
            throw new GigiApiException("Unknown ACE prefix.");
        }
        try {
            String unicode = IDN.toUnicode(label);
            if (unicode.startsWith("xn--")) {
                throw new GigiApiException("Punycode label could not be positively verified.");
            }
        } catch (IllegalArgumentException e) {
            throw new GigiApiException("Punycode label could not be positively verified.");
        }
    }

    public static boolean isVaildDomainPart(String s, boolean allowPunycode) {
        if ( !s.matches("[a-z0-9-]+")) {
            return false;
        }
        if (s.charAt(0) == '-' || s.charAt(s.length() - 1) == '-') {
            return false;
        }
        if (s.length() > 63) {
            return false;
        }
        boolean canBePunycode = s.length() >= 4 && s.charAt(2) == '-' && s.charAt(3) == '-';
        if (canBePunycode && !allowPunycode) {
            return false;
        }
        return true;
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
                throw new GigiApiException("Domain could not be inserted. Domain is already valid.");
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
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `domains` SET `deleted`=CURRENT_TIMESTAMP WHERE `id`=?")) {
            ps.setInt(1, id);
            ps.execute();
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

    public static synchronized Domain getById(int id) throws IllegalArgumentException {
        Domain em = myCache.get(id);
        if (em == null) {
            myCache.put(em = new Domain(id));
        }
        return em;
    }

    public static Domain searchUserIdByDomain(String domain) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id` FROM `domains` WHERE `domain` = ?")) {
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
