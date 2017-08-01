package club.wpia.gigi.database;

import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class GigiPreparedStatement implements AutoCloseable {

    private PreparedStatement target;

    private GigiResultSet rs;

    protected GigiPreparedStatement(PreparedStatement preparedStatement) {
        target = preparedStatement;
    }

    public GigiPreparedStatement(String stmt) {
        this(stmt, false);
    }

    public GigiPreparedStatement(String stmt, boolean scroll) {
        try {
            target = DatabaseConnection.getInstance().prepareInternal(stmt, scroll);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    public GigiResultSet executeQuery() {
        try {
            return rs = new GigiResultSet(target.executeQuery());
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public void executeUpdate() {
        try {
            int updated = target.executeUpdate();
            if (updated != 1) {
                throw new Error("FATAL: multiple or no data updated: " + updated);
            }
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    public boolean executeMaybeUpdate() {
        try {
            int updated = target.executeUpdate();
            if (updated > 1) {
                throw new Error("More than one record (" + updated + ") updated.");
            }
            return updated == 1;
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

    public void setEnum(int parameterIndex, DBEnum x) {
        try {
            target.setString(parameterIndex, x.getDBName());
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

    public ParameterMetaData getParameterMetaData() {
        try {
            return target.getParameterMetaData();
        } catch (SQLException e) {
            handleSQL(e);
            throw new Error(e);
        }
    }

    private void handleSQL(SQLException e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        GigiResultSet r = rs;
        if (r != null) {
            r.close();
        }
        PreparedStatement tg = target;
        target = null;
        try {
            DatabaseConnection.getInstance().returnStatement(tg);
        } catch (SQLException e) {
            throw new Error(e);
        }

    }

}
