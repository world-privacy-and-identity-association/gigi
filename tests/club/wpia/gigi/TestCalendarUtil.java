package club.wpia.gigi;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

import club.wpia.gigi.util.CalendarUtil;
import club.wpia.gigi.util.DayDate;

public class TestCalendarUtil {

    @Test
    public void testGetDateFromComponents() {

        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("UTC"));

        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        int days = now.get(Calendar.DATE);
        now.setTimeInMillis(0);
        now.set(year, month - 1, days, 0, 0, 0);

        DayDate dob = CalendarUtil.getDateFromComponents(year, month, days);
        DayDate d = new DayDate(now.getTimeInMillis());

        assertEquals(d.getTime(), dob.getTime());
        dob = CalendarUtil.getDateFromComponents(year + 1, month, days);

        assertNotEquals(d.getTime(), dob.getTime());

    }

    @Test
    public void testIsOfAge() {

        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        int days = now.get(Calendar.DATE);

        DayDate dob = CalendarUtil.getDateFromComponents(year - 14, month, days);

        assertTrue(CalendarUtil.isOfAge(dob, 13));

        assertTrue(CalendarUtil.isOfAge(dob, 14));
        // We need one day as safety margin. Between 10:00 and 23:59 UTC there
        // is a place on earth (UTC+1 to UTC+14) where a person having
        // birthday "tomorrow" is already of that age. So we need the day after
        // tomorrow for doing this check the easy way.
        dob = CalendarUtil.getDateFromComponents(year - 14, month, days + 2);
        assertFalse(CalendarUtil.isOfAge(dob, 14));

    }

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testIsDateValid() {
        assertTrue(CalendarUtil.isDateValid(2016, 2, 28));
        assertTrue(CalendarUtil.isDateValid(2016, 2, 29));
        assertFalse(CalendarUtil.isDateValid(2016, 2, 30));
        assertFalse(CalendarUtil.isDateValid(2016, 4, 31));

        assertTrue(CalendarUtil.isDateValid(2000, 2, 28));
        assertTrue(CalendarUtil.isDateValid(2000, 2, 29));
        assertFalse(CalendarUtil.isDateValid(2000, 2, 30));
        assertFalse(CalendarUtil.isDateValid(2000, 4, 31));

        assertTrue(CalendarUtil.isDateValid(2015, 2, 28));
        assertFalse(CalendarUtil.isDateValid(2015, 2, 29));
        assertFalse(CalendarUtil.isDateValid(2015, 2, 30));
        assertFalse(CalendarUtil.isDateValid(2015, 4, 31));

    }

}
