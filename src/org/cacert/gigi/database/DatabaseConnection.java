package org.cacert.gigi.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.database.SQLFileManager.ImportType;

public class DatabaseConnection {

    public static final int CURRENT_SCHEMA_VERSION = 4;

    public static final int CONNECTION_TIMEOUT = 24 * 60 * 60;

    private Connection c;

    private HashMap<String, GigiPreparedStatement> statements = new HashMap<String, GigiPreparedStatement>();

    private static Properties credentials;

    private Statement adHoc;

    public DatabaseConnection() {
        try {
            Class.forName(credentials.getProperty("sql.driver"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        tryConnect();

    }

    private void tryConnect() {
        try {
            c = DriverManager.getConnection(credentials.getProperty("sql.url") + "?socketTimeout=" + CONNECTION_TIMEOUT, credentials.getProperty("sql.user"), credentials.getProperty("sql.password"));
            adHoc = c.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public GigiPreparedStatement prepare(String query) {
        ensureOpen();
        query = preprocessQuery(query);
        GigiPreparedStatement statement = statements.get(query);
        if (statement == null) {
            try {
                statement = new GigiPreparedStatement(c.prepareStatement(query, query.startsWith("SELECT ") ? Statement.NO_GENERATED_KEYS : Statement.RETURN_GENERATED_KEYS));
            } catch (SQLException e) {
                throw new Error(e);
            }
            statements.put(query, statement);
        }
        return statement;
    }

    public GigiPreparedStatement prepareScrollable(String query) {
        ensureOpen();
        query = preprocessQuery(query);
        GigiPreparedStatement statement = statements.get(query);
        if (statement == null) {
            try {
                statement = new GigiPreparedStatement(c.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
            } catch (SQLException e) {
                throw new Error(e);
            }
            statements.put(query, statement);
        }
        return statement;
    }

    private long lastAction = System.currentTimeMillis();

    private void ensureOpen() {
        if (System.currentTimeMillis() - lastAction > CONNECTION_TIMEOUT * 1000L) {
            try {
                ResultSet rs = adHoc.executeQuery("SELECT 1");
                rs.close();
                lastAction = System.currentTimeMillis();
                return;
            } catch (SQLException e) {
            }
            statements.clear();
            tryConnect();
        }
        lastAction = System.currentTimeMillis();
    }

    private static ThreadLocal<DatabaseConnection> instances = new ThreadLocal<DatabaseConnection>() {

        @Override
        protected DatabaseConnection initialValue() {
            return new DatabaseConnection();
        }
    };

    public static DatabaseConnection getInstance() {
        return instances.get();
    }

    public static boolean isInited() {
        return credentials != null;
    }

    public static void init(Properties conf) {
        if (credentials != null) {
            throw new Error("Re-initiaizing is forbidden.");
        }
        credentials = conf;
        GigiResultSet rs = getInstance().prepare("SELECT version FROM \"schemeVersion\" ORDER BY version DESC LIMIT 1;").executeQuery();
        int version = 0;
        if (rs.next()) {
            version = rs.getInt(1);
        }
        if (version == CURRENT_SCHEMA_VERSION) {
            return; // Good to go
        }
        if (version > CURRENT_SCHEMA_VERSION) {
            throw new Error("Invalid database version. Please fix this.");
        }
        upgrade(version);
    }

    public void beginTransaction() throws SQLException {
        c.setAutoCommit(false);
    }

    private static void upgrade(int version) {
        try {
            Statement s = getInstance().c.createStatement();
            try {
                while (version < CURRENT_SCHEMA_VERSION) {
                    try (InputStream resourceAsStream = DatabaseConnection.class.getResourceAsStream("upgrade/from_" + version + ".sql")) {
                        if (resourceAsStream == null) {
                            throw new Error("Upgrade script from version " + version + " was not found.");
                        }
                        SQLFileManager.addFile(s, resourceAsStream, ImportType.PRODUCTION);
                    }
                    version++;
                }
                s.addBatch("UPDATE schemeVersion SET version='" + version + "'");
                System.out.println("UPGRADING Database to version " + version);
                s.executeBatch();
                System.out.println("done.");
            } finally {
                s.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void commitTransaction() throws SQLException {
        c.commit();
        c.setAutoCommit(true);
    }

    public void quitTransaction() {
        try {
            if ( !c.getAutoCommit()) {
                c.rollback();
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static final String preprocessQuery(String originalQuery) {
        originalQuery = originalQuery.replace('`', '"');
        if (originalQuery.matches("^INSERT INTO [^ ]+ SET .*")) {
            Pattern p = Pattern.compile("INSERT INTO ([^ ]+) SET (.*)");
            Matcher m = p.matcher(originalQuery);
            if (m.matches()) {
                String replacement = "INSERT INTO " + toIdentifier(m.group(1));
                String[] parts = m.group(2).split(",");
                StringJoiner columns = new StringJoiner(", ");
                StringJoiner values = new StringJoiner(", ");
                for (int i = 0; i < parts.length; i++) {
                    String[] split = parts[i].split("=", 2);
                    columns.add(toIdentifier(split[0]));
                    values.add(split[1]);
                }
                replacement += "(" + columns.toString() + ") VALUES(" + values.toString() + ")";
                return replacement;
            }
        }

        //
        return originalQuery;
    }

    private static CharSequence toIdentifier(String ident) {
        ident = ident.trim();
        if ( !ident.startsWith("\"")) {
            ident = "\"" + ident;
        }
        if ( !ident.endsWith("\"")) {
            ident = ident + "\"";
        }
        return ident;
    }
}
