package org.cacert.gigi;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.cacert.gigi.util.CalendarUtil;
import org.cacert.gigi.util.DayDate;
import org.junit.Test;

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

        dob = CalendarUtil.getDateFromComponents(year - 14, month, days + 1);
        assertFalse(CalendarUtil.isOfAge(dob, 14));

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
