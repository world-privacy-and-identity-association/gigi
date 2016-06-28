package org.cacert.gigi.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.database.SQLFileManager.ImportType;

public class DatabaseConnection {

    public static class Link implements AutoCloseable {

        private DatabaseConnection target;

        protected Link(DatabaseConnection target) {
            this.target = target;
        }

        @Override
        public void close() {
            synchronized (DatabaseConnection.class) {
                Link i = instances.get(Thread.currentThread());
                if (i != this) {
                    throw new Error();
                }
                instances.remove(Thread.currentThread());
                pool.add(target);
            }
        }

    }

    public static final int MAX_CACHED_INSTANCES = 3;

    private static class StatementDescriptor {

        String query;

        boolean scrollable;

        int instance;

        PreparedStatement target;

        public StatementDescriptor(String query, boolean scrollable) {
            this.query = query;
            this.scrollable = scrollable;
            this.instance = 0;
        }

        public synchronized void instanciate(Connection c) throws SQLException {
            if (scrollable) {
                target = c.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            } else {
                target = c.prepareStatement(query, query.startsWith("SELECT ") ? Statement.NO_GENERATED_KEYS : Statement.RETURN_GENERATED_KEYS);
            }

        }

        public synchronized PreparedStatement getTarget() {
            return target;
        }

        public synchronized void increase() {
            if (target != null) {
                throw new IllegalStateException();
            }
            instance++;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + instance;
            result = prime * result + ((query == null) ? 0 : query.hashCode());
            result = prime * result + (scrollable ? 1231 : 1237);
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
            StatementDescriptor other = (StatementDescriptor) obj;
            if (instance != other.instance) {
                return false;
            }
            if (query == null) {
                if (other.query != null) {
                    return false;
                }
            } else if ( !query.equals(other.query)) {
                return false;
            }
            if (scrollable != other.scrollable) {
                return false;
            }
            return true;
        }

    }

    public static final int CURRENT_SCHEMA_VERSION = 15;

    public static final int CONNECTION_TIMEOUT = 24 * 60 * 60;

    private Connection c;

    private HashMap<StatementDescriptor, PreparedStatement> statements = new HashMap<StatementDescriptor, PreparedStatement>();

    private HashSet<PreparedStatement> underUse = new HashSet<>();

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

    protected synchronized PreparedStatement prepareInternal(String query) throws SQLException {
        return prepareInternal(query, false);
    }

    protected synchronized PreparedStatement prepareInternal(String query, boolean scrollable) throws SQLException {

        ensureOpen();
        query = preprocessQuery(query);
        StatementDescriptor searchHead = new StatementDescriptor(query, scrollable);
        PreparedStatement statement = null;
        while (statement == null) {
            statement = statements.get(searchHead);
            if (statement == null) {
                searchHead.instanciate(c);
                statement = searchHead.getTarget();
                if (searchHead.instance >= MAX_CACHED_INSTANCES) {
                    return statement;
                }
                underUse.add(statement);
                statements.put(searchHead, statement);
            } else if (underUse.contains(statement)) {
                searchHead.increase();
                statement = null;
            } else {
                underUse.add(statement);
            }
        }
        return statement;
    }

    protected synchronized PreparedStatement prepareInternalScrollable(String query) throws SQLException {
        return prepareInternal(query, true);
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

    private static HashMap<Thread, Link> instances = new HashMap<>();

    private static LinkedBlockingDeque<DatabaseConnection> pool = new LinkedBlockingDeque<>();

    private static int connCount = 0;

    public static synchronized DatabaseConnection getInstance() {
        Link l = instances.get(Thread.currentThread());
        if (l == null) {
            throw new Error("No database connection allocated");
        }
        return l.target;
    }

    public static synchronized boolean hasInstance() {
        Link l = instances.get(Thread.currentThread());
        return l != null;
    }

    public static boolean isInited() {
        return credentials != null;
    }

    public static void init(Properties conf) {
        if (credentials != null) {
            throw new Error("Re-initiaizing is forbidden.");
        }
        credentials = conf;
        try (Link i = newLink(false)) {
            int version = 0;
            try (GigiPreparedStatement gigiPreparedStatement = new GigiPreparedStatement("SELECT version FROM \"schemeVersion\" ORDER BY version DESC LIMIT 1;")) {
                GigiResultSet rs = gigiPreparedStatement.executeQuery();
                if (rs.next()) {
                    version = rs.getInt(1);
                }
            }
            if (version == CURRENT_SCHEMA_VERSION) {
                return; // Good to go
            }
            if (version > CURRENT_SCHEMA_VERSION) {
                throw new Error("Invalid database version. Please fix this.");
            }
            upgrade(version);
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    private static void upgrade(int version) {
        try {
            Statement s = getInstance().c.createStatement();
            try {
                while (version < CURRENT_SCHEMA_VERSION) {
                    addUpgradeScript(Integer.toString(version), s);
                    version++;
                }
                s.addBatch("UPDATE \"schemeVersion\" SET version='" + version + "'");
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

    private static void addUpgradeScript(String version, Statement s) throws Error, IOException, SQLException {
        try (InputStream resourceAsStream = DatabaseConnection.class.getResourceAsStream("upgrade/from_" + version + ".sql")) {
            if (resourceAsStream == null) {
                throw new Error("Upgrade script from version " + version + " was not found.");
            }
            SQLFileManager.addFile(s, resourceAsStream, ImportType.PRODUCTION);
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

    protected synchronized void returnStatement(PreparedStatement target) throws SQLException {
        if ( !underUse.remove(target)) {
            target.close();
        }
    }

    public synchronized int getNumberOfLockedStatements() {
        return underUse.size();
    }

    public synchronized void lockedStatements(PrintWriter writer) {
        writer.println(underUse.size());
        for (PreparedStatement ps : underUse) {
            for (Entry<StatementDescriptor, PreparedStatement> e : statements.entrySet()) {
                if (e.getValue() == ps) {
                    writer.println("<br/>");
                    writer.println(e.getKey().instance + ":");

                    writer.println(e.getKey().query);
                }
            }
        }
    }

    public static synchronized Link newLink(boolean readOnly) throws InterruptedException {
        if (instances.get(Thread.currentThread()) != null) {
            throw new Error("There is already a connection allocated for this thread.");
        }
        if (pool.isEmpty() && connCount < 5) {
            pool.addLast(new DatabaseConnection());
            connCount++;
        }
        DatabaseConnection conn = pool.takeFirst();
        try {
            conn.c.setReadOnly(readOnly);
        } catch (SQLException e) {
            throw new Error(e);
        }
        Link l = new Link(conn);
        instances.put(Thread.currentThread(), l);
        return l;
    }
}
