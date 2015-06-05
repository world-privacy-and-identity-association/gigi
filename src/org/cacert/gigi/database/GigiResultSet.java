package org.cacert.gigi.database;

import java.io.Closeable;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class GigiResultSet implements Closeable {

    private ResultSet target;

    public GigiResultSet(ResultSet target) {
        this.target = target;
    }

    public String getString(int columnIndex) {
        try {
            return target.getString(columnIndex);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public boolean getBoolean(int columnIndex) {
        try {
            return target.getBoolean(columnIndex);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public int getInt(int columnIndex) {
        try {
            return target.getInt(columnIndex);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public Date getDate(int columnIndex) {
        try {
            return target.getDate(columnIndex);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public Timestamp getTimestamp(int columnIndex) {
        try {
            return target.getTimestamp(columnIndex);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public String getString(String columnLabel) {
        try {
            return target.getString(columnLabel);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public boolean getBoolean(String columnLabel) {
        try {
            return target.getBoolean(columnLabel);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public int getInt(String columnLabel) {
        try {
            return target.getInt(columnLabel);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public Date getDate(String columnLabel) {
        try {
            return target.getDate(columnLabel);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public Timestamp getTimestamp(String columnLabel) {
        try {
            return target.getTimestamp(columnLabel);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public boolean next() {
        try {
            return target.next();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public int getRow() {
        try {
            return target.getRow();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void beforeFirst() {
        try {
            target.beforeFirst();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void last() {
        try {
            target.last();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void close() {
        try {
            target.close();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }

    }

    private void handleSQL(SQLException e) {
        // TODO Auto-generated method stub

    }

}
