package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.TranslateCommand;

public enum Group {
    SUPPORTER("supporter", "supporter", true), ARBITRATOR("arbitrator", "arbitrator", true), //
    BLOCKEDASSURER("blockedassurer", "may not verify", true), BLOCKEDASSUREE("blockedassuree", "may not be verified", true), //
    BLOCKEDLOGIN("blockedlogin", "may not login", true), BLOCKEDCERT("blockedcert", "may not issue certificates", true), //
    TTP_ASSURER("ttp-assurer", "may verify via TTP", true), TTP_APPLICANT("ttp-applicant", "requests to be verified via ttp", true), //
    CODESIGNING("codesigning", "may issue codesigning certificates", true), ORGASSURER("orgassurer", "may verify organisations", true), //
    NUCLEUS_ASSURER("nucleus-assurer", "may enter nucleus verifications", true), LOCATE_AGENT("locate-agent", "wants access to the locate agent system", false);

    private final String dbName;

    private final TranslateCommand tc;

    private final boolean managedBySupport; // true if flag is handled by
                                            // support, false if handled by user

    private Group(String name, String display, boolean managedBySupport) {
        dbName = name;
        tc = new TranslateCommand(display);
        this.managedBySupport = managedBySupport;
    }

    public static Group getByString(String name) {
        return valueOf(name.toUpperCase().replace('-', '_'));
    }

    public boolean isManagedBySupport() {
        return managedBySupport;
    }

    public String getDatabaseName() {
        return dbName;
    }

    public User[] getMembers(int offset, int count) {
        try (GigiPreparedStatement gps = new GigiPreparedStatement("SELECT `user` FROM `user_groups` WHERE `permission`=?::`userGroup` AND `deleted` IS NULL OFFSET ? LIMIT ?", true)) {
            gps.setString(1, dbName);
            gps.setInt(2, offset);
            gps.setInt(3, count);
            GigiResultSet grs = gps.executeQuery();
            grs.last();
            User[] users = new User[grs.getRow()];
            int i = 0;
            grs.beforeFirst();
            while (grs.next()) {
                users[i++] = User.getById(grs.getInt(1));
            }
            return users;
        }
    }

    public Outputable getName() {
        return tc;
    }
}
