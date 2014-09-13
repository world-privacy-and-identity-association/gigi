package org.cacert.gigi.database;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class GigiPreparedStatement {

    PreparedStatement target;

    public GigiPreparedStatement(PreparedStatement preparedStatement) {
        target = preparedStatement;
    }

    public GigiResultSet executeQuery() {
        try {
            return new GigiResultSet(target.executeQuery());
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public int executeUpdate() {
        try {
            return target.executeUpdate();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public boolean execute() {
        try {
            return target.execute();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void setInt(int parameterIndex, int x) {
        try {
            target.setInt(parameterIndex, x);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void setString(int parameterIndex, String x) {
        try {
            target.setString(parameterIndex, x);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void setDate(int parameterIndex, Date x) {
        try {
            target.setDate(parameterIndex, x);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void setTimestamp(int parameterIndex, Timestamp x) {
        try {
            target.setTimestamp(parameterIndex, x);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public int lastInsertId() {
        try {
            ResultSet rs = target.getGeneratedKeys();
            rs.next();
            int id = rs.getInt(1);
            rs.close();
            return id;
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void setBoolean(int parameterIndex, boolean x) {
        try {
            target.setBoolean(parameterIndex, x);
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    private void handleSQL(SQLException e) {
        // TODO Auto-generated method stub

    }
}
