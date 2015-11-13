package org.cacert.gigi.database;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLFileManager {

    public static enum ImportType {
        /**
         * Execute Script as-is
         */
        PRODUCTION,
        /**
         * Execute Script, but change Engine=InnoDB to Engine=Memory
         */
        TEST,
        /**
         * Execute INSERT statements as-is, and TRUNCATE instead of DROPPING
         */
        TRUNCATE
    }

    public static void addFile(Statement stmt, InputStream f, ImportType type) throws IOException, SQLException {
        String sql = readFile(f);
        sql = sql.replaceAll("--[^\n]*\n", "\n");
        sql = sql.replaceAll("#[^\n]*\n", "\n");
        String[] stmts = sql.split(";");
        Pattern p = Pattern.compile("\\s*DROP TABLE IF EXISTS \"([^\"]+)\"");
        for (String string : stmts) {
            Matcher m = p.matcher(string);
            string = string.trim();
            if (string.equals("")) {
                continue;
            }
            if ((string.contains("profiles") || string.contains("cacerts") || string.contains("cats_type")) && type != ImportType.PRODUCTION) {
                continue;
            }
            string = DatabaseConnection.preprocessQuery(string);
            if (m.matches() && type == ImportType.TRUNCATE) {
                String sql2 = "DELETE FROM \"" + m.group(1) + "\"";
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

    private static String readFile(InputStream f) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] buf = new byte[4096];
        while ((len = f.read(buf)) > 0) {
            baos.write(buf, 0, len);
        }
        return new String(baos.toByteArray(), "UTF-8");
    }
}
