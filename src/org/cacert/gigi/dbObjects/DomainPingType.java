package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.DBEnum;

public enum DomainPingType implements DBEnum {
    EMAIL, DNS, HTTP, SSL;

    @Override
    public String getDBName() {
        return toString().toLowerCase();
    }
}
