package org.cacert.gigi.dbObjects;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public class CATS {

    private static HashMap<String, Integer> names = new HashMap<>();

    public static final String ASSURER_CHALLANGE_NAME = "Assurer's Challange";

    public static final int ASSURER_CHALLANGE_ID;

    private CATS() {

    }

    static {
        GigiResultSet res = DatabaseConnection.getInstance().prepare("SELECT `id`, `type_text` FROM `cats_type`").executeQuery();
        while (res.next()) {
            names.put(res.getString(2), res.getInt(1));
        }
        ASSURER_CHALLANGE_ID = getID(ASSURER_CHALLANGE_NAME);
    }

    public static synchronized int getID(String name) {
        Integer i = names.get(name);
        if (i == null) {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `cats_type` SET `type_text`=?");
            ps.setString(1, name);
            ps.execute();
            i = ps.lastInsertId();
            names.put(name, i);
        }
        return i;
    }

    public static void enterResult(User user, String testType, Date passDate) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `cats_passed` SET `user_id`=?, `variant_id`=?, `pass_date`=?");
        ps.setInt(1, user.getId());
        ps.setInt(2, getID(testType));
        ps.setTimestamp(3, new Timestamp(passDate.getTime()));
        ps.execute();
    }
}
