package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public class DomainPingConfiguration implements IdCachable {

    public static enum PingType {
        EMAIL, DNS, HTTP, SSL;
    }

    private int id;

    private Domain target;

    private PingType type;

    private String info;

    private DomainPingConfiguration(int id) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id, domainid, type, info FROM pingconfig WHERE id=?");
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

    public void requestReping() {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE pingconfig set reping='y' WHERE id=?");
        ps.setInt(1, id);
        ps.execute();
    }

}
