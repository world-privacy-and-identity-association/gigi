package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.GigiResultSet;

public class DomainPingExecution {

    private String state;

    private String type;

    private String info;

    private String result;

    private DomainPingConfiguration config;

    public DomainPingExecution(GigiResultSet rs) {
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
