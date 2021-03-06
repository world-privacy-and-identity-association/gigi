package club.wpia.gigi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.SQLFileManager;
import club.wpia.gigi.database.SQLFileManager.ImportType;

public class DatabaseManager {

    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
        boolean test = false;
        if (args.length >= 1 && args[0].equals("--test")) {
            test = true;
            String[] ne = new String[args.length - 1];
            System.arraycopy(args, 1, ne, 0, ne.length);
            args = ne;
        }
        if (args.length == 0) {
            Properties p = new Properties();
            try (Reader reader = new InputStreamReader(new FileInputStream("config/gigi.properties"), "UTF-8")) {
                p.load(reader);
            }
            args = new String[] {
                    p.getProperty("sql.driver"), p.getProperty("sql.url"), p.getProperty("sql.user"), p.getProperty("sql.password")
            };
        }
        if (args.length < 4) {
            System.err.println("Usage: org.postgresql.Driver jdbc:postgresql://localhost/gigi user password");
            return;
        }
        run(args, test ? ImportType.TEST : ImportType.PRODUCTION);
    }

    public static void run(String[] args, ImportType truncate) throws ClassNotFoundException, SQLException, IOException {
        Class.forName(args[0]);
        final Connection conn = DriverManager.getConnection(args[1], args[2], args[3]);
        try {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            try {
                try (InputStream structure = DatabaseConnection.class.getResourceAsStream("tableStructure.sql")) {
                    SQLFileManager.addFile(stmt, structure, truncate);
                }
                File localData = new File("doc/sampleData.sql");
                if (localData.exists()) {
                    try (FileInputStream f = new FileInputStream(localData)) {
                        SQLFileManager.addFile(stmt, f, ImportType.SAMPLE_DATA);
                    }
                }
                stmt.executeBatch();
                conn.commit();
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            e.getNextException().printStackTrace();
            throw e;
        } finally {
            conn.close();
        }
    }
}
