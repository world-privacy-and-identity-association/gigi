package org.cacert.gigi.dbObjects;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.dbObjects.DomainPingConfiguration.PingType;

public class Domain implements IdCachable {

    public class DomainPingExecution {

        private String state;

        private String type;

        private String info;

        private String result;

        private DomainPingConfiguration config;

        public DomainPingExecution(ResultSet rs) throws SQLException {
            state = rs.getString(1);
            type = rs.getString(2);
            info = rs.getString(3);
            result = rs.getString(4);
            config = DomainPingConfiguration.getById(rs.getInt(5));
        }

        public String getState() {
            return state;
        }

        public String getType() {
            return type;
        }

        public String getInfo() {
            return info;
        }

        public String getResult() {
            return result;
        }

        public DomainPingConfiguration getConfig() {
            return config;
        }

    }

    private User owner;

    private String suffix;

    private int id;

    private Domain(int id) throws SQLException {
        PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT memid, domain FROM `domains` WHERE id=? AND deleted IS NULL");
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if ( !rs.next()) {
            throw new IllegalArgumentException("Invalid domain id " + id);
        }
        this.id = id;
        owner = User.getById(rs.getInt(1));
        suffix = rs.getString(2);
        rs.close();
    }

    public Domain(User owner, String suffix) throws GigiApiException {
        this.owner = owner;
        this.suffix = suffix;

    }

    private static void checkInsert(String suffix) throws GigiApiException {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT 1 FROM `domains` WHERE (domain=RIGHT(?,LENGTH(domain))  OR RIGHT(domain,LENGTH(?))=?) AND deleted IS NULL");
            ps.setString(1, suffix);
            ps.setString(2, suffix);
            ps.setString(3, suffix);
            ResultSet rs = ps.executeQuery();
            boolean existed = rs.next();
            rs.close();
            if (existed) {
                throw new GigiApiException("Domain could not be inserted. Domain is already valid.");
            }
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }
    }

    public void insert() throws GigiApiException {
        if (id != 0) {
            throw new GigiApiException("already inserted.");
        }
        synchronized (Domain.class) {
            checkInsert(suffix);
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `domains` SET memid=?, domain=?");
                ps.setInt(1, owner.getId());
                ps.setString(2, suffix);
                ps.execute();
                id = DatabaseConnection.lastInsertId(ps);
                myCache.put(this);
            } catch (SQLException e) {
                throw new GigiApiException(e);
            }
        }
    }

    public void delete() throws GigiApiException {
        if (id == 0) {
            throw new GigiApiException("not inserted.");
        }
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE `domains` SET deleted=CURRENT_TIMESTAMP WHERE id=?");
            ps.setInt(1, id);
            ps.execute();
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }
    }

    public User getOwner() {
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
            try {
                configs = new LinkedList<>();
                PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id FROM pingconfig WHERE domainid=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    configs.add(DomainPingConfiguration.getById(rs.getInt(1)));
                }
                rs.close();
                this.configs = configs;
            } catch (SQLException e) {
                throw new GigiApiException(e);
            }

        }
        return Collections.unmodifiableList(configs);
    }

    public void addPing(PingType ssl, String config) throws GigiApiException {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO pingconfig SET domainid=?, type=?, info=?");
            ps.setInt(1, id);
            ps.setString(2, ssl.toString().toLowerCase());
            ps.setString(3, config);
            ps.execute();
            configs = null;
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }
    }

    public void verify(String hash) throws GigiApiException {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE domainPinglog SET state='success' WHERE challenge=? AND configId IN (SELECT id FROM pingconfig WHERE domainId=?)");
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }
    }

    public boolean isVerified() {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT 1 FROM domainPinglog INNER JOIN pingconfig ON pingconfig.id=domainPinglog.configId WHERE domainid=? AND state='success'");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public DomainPingExecution[] getPings() throws GigiApiException {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT state, type, info, result, configId FROM domainPinglog INNER JOIN pingconfig ON pingconfig.id=domainPinglog.configid WHERE pingconfig.domainid=? ORDER BY `when` DESC;");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            rs.last();
            DomainPingExecution[] contents = new DomainPingExecution[rs.getRow()];
            rs.beforeFirst();
            for (int i = 0; i < contents.length && rs.next(); i++) {
                contents[i] = new DomainPingExecution(rs);
            }
            return contents;
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }

    }

    private static ObjectCache<Domain> myCache = new ObjectCache<>();

    public static synchronized Domain getById(int id) throws IllegalArgumentException {
        Domain em = myCache.get(id);
        if (em == null) {
            try {
                myCache.put(em = new Domain(id));
            } catch (SQLException e1) {
                throw new IllegalArgumentException(e1);
            }
        }
        return em;
    }

}
