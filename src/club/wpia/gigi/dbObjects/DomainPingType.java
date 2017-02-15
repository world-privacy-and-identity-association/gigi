package club.wpia.gigi.dbObjects;

import club.wpia.gigi.database.DBEnum;

public enum DomainPingType implements DBEnum {
    EMAIL, DNS, HTTP, SSL;

    @Override
    public String getDBName() {
        return toString().toLowerCase();
    }
}
