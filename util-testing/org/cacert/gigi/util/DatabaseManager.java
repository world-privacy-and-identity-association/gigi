package org.cacert.gigi.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {

    public static String readFile(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()));
    }

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

    public static enum ImportType {
        /**
         * Execute Script as-as
         */
        PRODUCTION,
        /**
         * Execute Script, but changing Engine=InnoDB to Engine=Memory
         */
        TEST,
        /**
         * Execute INSERT statements as-is, and TRUNCATE instead of DROPPING
         */
        TRUNCATE
    }

    public static void run(String[] args, ImportType truncate) throws ClassNotFoundException, SQLException, IOException {
        Class.forName(args[0]);
        Connection conn = DriverManager.getConnection(args[1], args[2], args[3]);
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement();
        addFile(stmt, new File("doc/tableStructure.sql"), truncate);
        File localData = new File("doc/sampleData.sql");
        if (localData.exists()) {
            addFile(stmt, localData, ImportType.PRODUCTION);
        }
        stmt.executeBatch();
        conn.commit();
        stmt.close();
    }

    private static void addFile(Statement stmt, File f, ImportType type) throws IOException, SQLException {
        String sql = readFile(f);
        sql = sql.replaceAll("--[^\n]+\n", "\n");
        String[] stmts = sql.split(";");
        Pattern p = Pattern.compile("\\s*DROP TABLE IF EXISTS `([^`]+)`");
        for (String string : stmts) {
            Matcher m = p.matcher(string);
            string = string.trim();
            if (string.equals("")) {
                continue;
            }
            if (m.matches() && type == ImportType.TRUNCATE) {
                String sql2 = "TRUNCATE `" + m.group(1) + "`";
                stmt.addBatch(sql2);
                continue;
            }
            if (type == ImportType.PRODUCTION || string.startsWith("INSERT")) {
                stmt.addBatch(string);
            } else if (type == ImportType.TEST) {
                stmt.addBatch(string.replace("ENGINE=InnoDB", "ENGINE=Memory"));
            }
        }
    }
}