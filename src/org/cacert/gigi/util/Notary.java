package org.cacert.gigi.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.DateSelector;

public class Notary {

    public static void writeUserAgreement(int memid, String document, String method, String comment, boolean active, int secmemid) throws SQLException {
        PreparedStatement q = DatabaseConnection.getInstance().prepare("insert into `user_agreements` set `memid`=?, `secmemid`=?," + " `document`=?,`date`=NOW(), `active`=?,`method`=?,`comment`=?");
        q.setInt(1, memid);
        q.setInt(2, secmemid);
        q.setString(3, document);
        q.setInt(4, active ? 1 : 0);
        q.setString(5, method);
        q.setString(6, comment);
        q.execute();
    }

    public static void checkAssuranceIsPossible(User assurer, User target) throws GigiApiException {
        if (assurer.getId() == target.getId()) {
            throw new GigiApiException("You cannot assure yourself.");
        }
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT 1 FROM `notary` where `to`=? and `from`=? AND `deleted`=0");
            ps.setInt(1, target.getId());
            ps.setInt(2, assurer.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                rs.close();
                throw new GigiApiException("You have already assured this member.");
            }
            rs.close();
            if ( !assurer.canAssure()) {
                throw new GigiApiException("You are not an assurer.");
            }
        } catch (SQLException e) {
            throw new GigiApiException(e);
        }
    }

    public synchronized static void assure(User assurer, User target, int awarded, String location, String date) throws SQLException, GigiApiException {
        GigiApiException gae = new GigiApiException();

        if (date == null || date.equals("")) {
            gae.mergeInto(new GigiApiException("You must enter the date when you met the assuree."));
        } else {
            try {
                Date d = DateSelector.getDateFormat().parse(date);
                if (d.getTime() > System.currentTimeMillis()) {
                    gae.mergeInto(new GigiApiException("You must not enter a date in the future."));
                }
            } catch (ParseException e) {
                gae.mergeInto(new GigiApiException("You must enter the date in this format: YYYY-MM-DD."));
            }
        }
        // check location, min 3 characters
        if (location == null || location.equals("")) {
            gae.mergeInto(new GigiApiException("You failed to enter a location of your meeting."));
        } else if (location.length() <= 2) {
            gae.mergeInto(new GigiApiException("You must enter a location with at least 3 characters eg town and country."));
        }

        try {
            checkAssuranceIsPossible(assurer, target);
        } catch (GigiApiException e) {
            gae.mergeInto(e);
        }

        User u = new User(target.getId());
        if ( !u.equals(target)) {
            gae.mergeInto(new GigiApiException("The person you are assuring changed his personal details."));
        }
        if (awarded > assurer.getMaxAssurePoints() || awarded < 0) {
            gae.mergeInto(new GigiApiException("The points you are trying to award are out of range."));
        }
        if ( !gae.isEmpty()) {
            throw gae;
        }

        PreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?");
        ps.setInt(1, assurer.getId());
        ps.setInt(2, target.getId());
        ps.setInt(3, awarded);
        ps.setString(4, location);
        ps.setString(5, date);
        ps.execute();
        assurer.invalidateMadeAssurances();
        target.invalidateReceivedAssurances();
    }
}
