package org.cacert.gigi.dbObjects;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public class CATS {

    private static HashMap<String, Integer> names = new HashMap<>();

    public static final String ASSURER_CHALLENGE_NAME = "Assurer's Challenge";

    public static final int ASSURER_CHALLENGE_ID;

    private CATS() {

    }

    static {
        try (GigiPreparedStatement st = new GigiPreparedStatement("SELECT `id`, `type_text` FROM `cats_type`")) {
            GigiResultSet res = st.executeQuery();
            while (res.next()) {
                names.put(res.getString(2), res.getInt(1));
            }
        }
        ASSURER_CHALLENGE_ID = getID(ASSURER_CHALLENGE_NAME);
    }

    public static synchronized int getID(String name) {
        Integer i = names.get(name);
        if (i == null) {
            try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `cats_type` SET `type_text`=?")) {
                ps.setString(1, name);
                ps.execute();
                i = ps.lastInsertId();
            }
            names.put(name, i);
        }
        return i;
    }

    public static void enterResult(User user, String testType, Date passDate) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `cats_passed` SET `user_id`=?, `variant_id`=?, `pass_date`=?")) {
            ps.setInt(1, user.getId());
            ps.setInt(2, getID(testType));
            ps.setTimestamp(3, new Timestamp(passDate.getTime()));
            ps.execute();
        }
    }
}
