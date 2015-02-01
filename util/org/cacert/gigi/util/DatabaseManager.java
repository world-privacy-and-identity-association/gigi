package org.cacert.gigi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.SQLFileManager;
import org.cacert.gigi.database.SQLFileManager.ImportType;

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
            p.load(new FileReader("config/gigi.properties"));
            args = new String[] {
                    p.getProperty("sql.driver"), p.getProperty("sql.url"), p.getProperty("sql.user"), p.getProperty("sql.password")
            };
        }
        if (args.length < 4) {
            System.err.println("Usage: com.mysql.jdbc.Driver jdbc:mysql://localhost/cacert user password");
            return;
        }
        run(args, test ? ImportType.TEST : ImportType.PRODUCTION);
    }

    public static void run(String[] args, ImportType truncate) throws ClassNotFoundException, SQLException, IOException {
        Class.forName(args[0]);
        Connection conn = DriverManager.getConnection(args[1], args[2], args[3]);
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement();
        SQLFileManager.addFile(stmt, DatabaseConnection.class.getResourceAsStream("tableStructure.sql"), truncate);
        File localData = new File("doc/sampleData.sql");
        if (localData.exists()) {
            SQLFileManager.addFile(stmt, new FileInputStream(localData), ImportType.PRODUCTION);
        }
        stmt.executeBatch();
        conn.commit();
        stmt.close();
    }

}
