package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.DBEnum;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.TranslateCommand;

public enum Group implements DBEnum {
    SUPPORTER("supporter", "supporter", true, false, true), //
    ARBITRATOR("arbitrator", "arbitrator", true, false, true), //
    BLOCKEDASSURER("blockedassurer", "may not verify", true, false, false), //
    BLOCKEDASSUREE("blockedassuree", "may not be verified", true, false, false), //
    BLOCKEDLOGIN("blockedlogin", "may not login", true, false, false), //
    BLOCKEDCERT("blockedcert", "may not issue certificates", true, false, false), //
    TTP_ASSURER("ttp-assurer", "may verify via TTP", true, false, true), //
    TTP_APPLICANT("ttp-applicant", "requests to be verified via ttp", false, true, false), //
    CODESIGNING("codesigning", "may issue codesigning certificates", true, false, false), //
    ORGASSURER("orgassurer", "may verify organisations", true, false, true), //
    NUCLEUS_ASSURER("nucleus-assurer", "may enter nucleus verifications", true, false, true), //
    LOCATE_AGENT("locate-agent", "wants access to the locate agent system", false, true, false);

    private final String dbName;

    private final TranslateCommand tc;

    private final boolean managedBySupport;

    private final boolean managedByUser;

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
    private Group(String name, String display, boolean managedBySupport, boolean managedByUser, boolean isSelfViewable) {
        dbName = name;
        tc = new TranslateCommand(display);
        if (managedByUser && managedBySupport) {
            throw new IllegalArgumentException("We do not allow groups to be user and support managable.");
        }
        if (managedByUser && isSelfViewable) {
            throw new IllegalArgumentException("We do not allow groups to be self-viewable and managable by user.");
        }
        this.managedByUser = managedByUser;
        this.managedBySupport = managedBySupport;
        this.isSelfViewable = isSelfViewable;
    }

    public static Group getByString(String name) {
        return valueOf(name.toUpperCase().replace('-', '_'));
    }

    public boolean isManagedBySupport() {
        return managedBySupport;
    }

    public boolean isManagedByUser() {
        return managedByUser;
    }

    public boolean isSelfViewable() {
        return isSelfViewable;
    }

    public User[] getMembers(int offset, int count) {
        try (GigiPreparedStatement gps = new GigiPreparedStatement("SELECT `user` FROM `user_groups` WHERE `permission`=?::`userGroup` AND `deleted` IS NULL OFFSET ? LIMIT ?", true)) {
            gps.setEnum(1, this);
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
            gps.setEnum(1, this);
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

    @Override
    public String getDBName() {
        return dbName;
    }
}
