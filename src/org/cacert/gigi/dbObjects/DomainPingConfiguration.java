package org.cacert.gigi.dbObjects;

import java.util.Date;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.SprintfCommand;

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
}
