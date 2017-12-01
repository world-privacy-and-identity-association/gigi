package club.wpia.gigi.dbObjects;

import java.sql.Timestamp;
import java.util.Date;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.output.template.SprintfCommand;

public class DomainPingConfiguration implements IdCachable {

    private static final int REPING_MINIMUM_DELAY = 5 * 60 * 1000;

    private int id;

    private Domain target;

    private DomainPingType type;

    private String info;

    private DomainPingConfiguration(int id) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id`, `domainid`, `type`, `info` FROM `pingconfig` WHERE `id`=?")) {
            ps.setInt(1, id);

            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                throw new IllegalArgumentException("Invalid pingconfig id " + id);
            }
            this.id = rs.getInt("id");
            target = Domain.getById(rs.getInt("domainid"));
            type = DomainPingType.valueOf(rs.getString("type").toUpperCase());
            info = rs.getString("info");
        }
    }

    @Override
    public int getId() {
        return id;
    }

    public Domain getTarget() {
        return target;
    }

    public DomainPingType getType() {
        return type;
    }

    public String getInfo() {
        return info;
    }

    private static ObjectCache<DomainPingConfiguration> cache = new ObjectCache<>();

    public static synchronized DomainPingConfiguration getById(int id) {
        DomainPingConfiguration res = cache.get(id);
        if (res == null) {
            cache.put(res = new DomainPingConfiguration(id));
        }
        return res;
    }

    public Date getLastExecution() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `when` AS stamp from `domainPinglog` WHERE `configId`=? ORDER BY `when` DESC LIMIT 1")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Date(rs.getTimestamp("stamp").getTime());
            }
            return new Date(0);
        }
    }

    public Date getLastSuccess() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `when` AS stamp from `domainPinglog` WHERE `configId`=? AND state='success' ORDER BY `when` DESC LIMIT 1")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Date(rs.getTimestamp("stamp").getTime());
            }
            return new Date(0);
        }
    }

    public synchronized void requestReping() throws GigiApiException {
        Date lastExecution = getLastExecution();
        if (lastExecution.getTime() + REPING_MINIMUM_DELAY < System.currentTimeMillis()) {
            Gigi.notifyPinger(this);
            return;
        }
        throw new GigiApiException(SprintfCommand.createSimple("Reping is only allowed after {0} minutes, yours end at {1}.", REPING_MINIMUM_DELAY / 60 / 1000, new Date(lastExecution.getTime() + REPING_MINIMUM_DELAY)));
    }

    /**
     * Return true when there was a last execution and it succeeded.
     * 
     * @return if this ping is currently valid.
     */
    public boolean isValid() {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT state='success' AS bool from `domainPinglog` WHERE `configId`=? ORDER BY `when` DESC LIMIT 1")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                return false;
            }
            return rs.getBoolean(1);
        }
    }

    /**
     * Return true when this ping has not been successful within the last 2
     * weeks.
     * 
     * @param time
     *            the point in time for which the determination is carried out.
     * @return the value for this ping.
     */
    public boolean isStrictlyInvalid(Date time) {
        Date lastSuccess = getLastSuccess();
        if (lastSuccess.getTime() == 0) {
            // never a successful ping
            return true;
        }
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `when` AS stamp from `domainPinglog` WHERE `configId`=? AND state='failed' AND `when` > ? ORDER BY `when` ASC LIMIT 1")) {
            ps.setInt(1, id);
            ps.setTimestamp(2, new Timestamp(lastSuccess.getTime()));
            GigiResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Date turnedInvalid = new Date(rs.getTimestamp("stamp").getTime());
                // turned invalid older than 2 weeks ago
                return turnedInvalid.getTime() < time.getTime() - 2L * 7 * 24 * 60 * 60 * 1000;
            }
            return false;
        }
    }
}
