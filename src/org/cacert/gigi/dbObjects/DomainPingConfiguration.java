package org.cacert.gigi.dbObjects;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.Scope;
import org.cacert.gigi.output.template.SprintfCommand;

public class DomainPingConfiguration implements IdCachable {

    public static enum PingType {
        EMAIL, DNS, HTTP, SSL;
    }

    private int id;

    private Domain target;

    private PingType type;

    private String info;

    private DomainPingConfiguration(int id) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT `id`, `domainid`, `type`, `info` FROM `pingconfig` WHERE `id`=?");
        ps.setInt(1, id);

        GigiResultSet rs = ps.executeQuery();
        if ( !rs.next()) {
            throw new IllegalArgumentException("Invalid pingconfig id " + id);
        }
        this.id = rs.getInt("id");
        target = Domain.getById(rs.getInt("domainid"));
        type = PingType.valueOf(rs.getString("type").toUpperCase());
        info = rs.getString("info");
    }

    @Override
    public int getId() {
        return id;
    }

    public Domain getTarget() {
        return target;
    }

    public PingType getType() {
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
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT `when` AS stamp from domainPinglog WHERE configId=? ORDER BY `when` DESC LIMIT 1");
        ps.setInt(1, id);
        GigiResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Date(rs.getTimestamp("stamp").getTime());
        }
        return new Date(0);
    }

    public Date getLastSuccess() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT `when` AS stamp from domainPinglog WHERE configId=? AND state='success' ORDER BY `when` DESC LIMIT 1");
        ps.setInt(1, id);
        GigiResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Date(rs.getTimestamp("stamp").getTime());
        }
        return new Date(0);
    }

    public synchronized void requestReping() throws GigiApiException {
        Date lastExecution = getLastExecution();
        if (lastExecution.getTime() + 5 * 60 * 1000 < System.currentTimeMillis()) {
            Gigi.notifyPinger(this);
            return;
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("data", new Date(lastExecution.getTime() + 5 * 60 * 1000));
        throw new GigiApiException(new Scope(new SprintfCommand("Reping is only allowed after 5 minutes, yours end at {0}.", Arrays.asList("${data}")), data));
    }
}
