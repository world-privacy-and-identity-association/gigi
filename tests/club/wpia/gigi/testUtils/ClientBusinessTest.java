package club.wpia.gigi.testUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.util.TimeConditions;

public class ClientBusinessTest extends BusinessTest {

    protected final User u;

    protected final Name n0;

    protected final int id;

    public ClientBusinessTest() {
        try {
            id = createVerifiedUser("a", "b", createUniqueName() + "@example.com", TEST_PASSWORD);
            u = User.getById(id);
            n0 = u.getNames()[0];
        } catch (GigiApiException e) {
            throw new Error(e);
        }
    }

    public static void setVerificationDateToPast(Name name) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -TimeConditions.getInstance().getVerificationMonths());
        String date = sdf.format(new Date(c.getTimeInMillis()));
        GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `notary` SET `date`=? WHERE `to`=? AND `date`>?");
        ps.setString(1, date);
        ps.setInt(2, name.getId());
        ps.setString(3, date);
        ps.execute();
        ps.close();
    }
}
