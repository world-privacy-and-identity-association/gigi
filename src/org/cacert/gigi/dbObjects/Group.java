package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.TranslateCommand;

public enum Group {
    SUPPORTER("supporter"), ARBITRATOR("arbitrator"), BLOCKEDASSURER("blockedassurer"), BLOCKEDASSUREE("blockedassuree"), BLOCKEDLOGIN("blockedlogin"), BLOCKEDCERT("blockedcert"), TTP_ASSURER("ttp-assurer"), TTP_APPLICANT("ttp-applicant"), CODESIGNING("codesigning"), ORGASSURER("orgassurer"), NUCLEUS_ASSURER("nucleus-assurer");

    private final String dbName;

    private final TranslateCommand tc;

    private Group(String name) {
        dbName = name;
        tc = new TranslateCommand(name);
    }

    public static Group getByString(String name) {
        return valueOf(name.toUpperCase().replace('-', '_'));
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
