package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.DBEnum;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.wrappers.DataContainer;

@DataContainer
public class NamePart {

    public enum NamePartType implements DBEnum {
        FIRST_NAME, LAST_NAME, SINGLE_NAME, SUFFIX;

        public String getDBName() {
            return name().toLowerCase().replace("_", "-");
        }
    }

    private NamePartType type;

    private String value;

    public NamePart(NamePartType type, String value) {
        if (type == null || value == null || value.trim().isEmpty() || !value.trim().equals(value)) {
            throw new IllegalArgumentException();
        }
        this.type = type;
        this.value = value;
    }

    public NamePart(GigiResultSet rs1) {
        value = rs1.getString("value");
        type = NamePartType.valueOf(rs1.getString("type").replace("-", "_").toUpperCase());
    }

    public NamePartType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NamePart other = (NamePart) obj;
        if (type != other.type) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if ( !value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
