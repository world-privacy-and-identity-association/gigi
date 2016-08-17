package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.TranslateCommand;

public enum Group {
    SUPPORTER("supporter", "supporter", true, true), //
    ARBITRATOR("arbitrator", "arbitrator", true, true), //
    BLOCKEDASSURER("blockedassurer", "may not verify", true, false), //
    BLOCKEDASSUREE("blockedassuree", "may not be verified", true, false), //
    BLOCKEDLOGIN("blockedlogin", "may not login", true, false), //
    BLOCKEDCERT("blockedcert", "may not issue certificates", true, false), //
    TTP_ASSURER("ttp-assurer", "may verify via TTP", true, true), //
    TTP_APPLICANT("ttp-applicant", "requests to be verified via ttp", true, false), //
    CODESIGNING("codesigning", "may issue codesigning certificates", true, false), //
    ORGASSURER("orgassurer", "may verify organisations", true, true), //
    NUCLEUS_ASSURER("nucleus-assurer", "may enter nucleus verifications", true, true), //
    LOCATE_AGENT("locate-agent", "wants access to the locate agent system", false, false);

    private final String dbName;

    private final TranslateCommand tc;

    private final boolean managedBySupport;

    private final boolean isSelfViewable;

    /**
     * Creates a new group. Users can join this group or be put into it
     * (depending on the value of <code>managedBySupport</code>).
     * 
     * @param name
     *            name of the group, used in database
     * @param display
     *            text displayed to user
     * @param managedBySupport
     *            true if flag is handled by support, false if handled by user
     * @param isSelfViewable
     *            true iff user should be able to see others in the same group
     */
    private Group(String name, String display, boolean managedBySupport, boolean isSelfViewable) {
        dbName = name;
        tc = new TranslateCommand(display);
        this.managedBySupport = managedBySupport;
        this.isSelfViewable = isSelfViewable;
    }

    public static Group getByString(String name) {
        return valueOf(name.toUpperCase().replace('-', '_'));
    }

    public boolean isManagedBySupport() {
        return managedBySupport;
    }

    public boolean isSelfViewable() {
        return isSelfViewable;
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

    public int getMemberCount() {
        try (GigiPreparedStatement gps = new GigiPreparedStatement("SELECT COUNT(`user`) FROM `user_groups` WHERE `permission`=?::`userGroup` AND `deleted` IS NULL", true)) {
            gps.setString(1, dbName);
            GigiResultSet grs = gps.executeQuery();
            if ( !grs.next()) {
                return 0;
            }
            return grs.getInt(1);
        }
    }

    public Outputable getName() {
        return tc;
    }
}
