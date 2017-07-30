package club.wpia.gigi.dbObjects;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.util.TimeConditions;

public class CATS {

    public enum CATSType {
        AGENT_CHALLENGE("Agent Qualifying Challenge"),

        ORG_AGENT_CHALLENGE("Organisation Agent Qualifying Challenge"),

        TTP_AGENT_CHALLENGE("TTP Agent Qualifying Challenge"),

        TTP_TOPUP_AGENT_CHALLENGE_NAME("TTP TOPUP Agent Qualifying Challenge"),

        CODE_SIGNING_CHALLENGE_NAME("Code Signing Challenge"),

        ORG_ADMIN_DP_CHALLENGE_NAME("Organisation Administrator Data Protection Challenge"),

        SUPPORT_DP_CHALLENGE_NAME("Support Data Protection Challenge");

        private final String displayName;

        private final int id;

        private CATSType(String displayName) {
            this.displayName = displayName;
            id = getID(displayName);
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getId() {
            return id;
        }
    }

    /**
     * The maximal number of months a passed test is considered "recent".
     */
    public static final int TEST_MONTHS = TimeConditions.getInstance().getTestMonths();

    private static HashMap<String, Integer> names = new HashMap<>();

    private CATS() {

    }

    static {
        try (GigiPreparedStatement st = new GigiPreparedStatement("SELECT `id`, `type_text` FROM `cats_type`")) {
            GigiResultSet res = st.executeQuery();
            while (res.next()) {
                names.put(res.getString(2), res.getInt(1));
            }
        }
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

    public static void enterResult(User user, CATSType testType, Date passDate, String language, String version) {
        enterResult(user, testType.id, passDate, language, version);
    }

    public static void enterResult(User user, String testType, Date passDate, String language, String version) {
        enterResult(user, getID(testType), passDate, language, version);
    }

    private static void enterResult(User user, int testTypeId, Date passDate, String language, String version) {

        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `cats_passed` SET `user_id`=?, `variant_id`=?, `pass_date`=?, `language`=?, `version`=?")) {
            ps.setInt(1, user.getId());
            ps.setInt(2, testTypeId);
            ps.setTimestamp(3, new Timestamp(passDate.getTime()));
            ps.setString(4, language);
            ps.setString(5, version);
            ps.execute();
        }
    }

    public static boolean isInCatsLimit(int uID, int testID) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT 1 FROM `cats_passed` WHERE `user_id` = ? AND `variant_id` = ? AND`pass_date` > (now() - interval '1 months' * ?::INTEGER)")) {
            ps.setInt(1, uID);
            ps.setInt(2, testID);
            ps.setInt(3, TEST_MONTHS);

            GigiResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }
}
