package org.cacert.gigi.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class CalendarUtil {

    public static boolean isDateValid(int year, int month, int day) {

        Calendar c = GregorianCalendar.getInstance();
        c.set(year, month - 1, day);
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month - 1 && c.get(Calendar.DATE) == day;

    }

    public static DayDate getDateFromComponents(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day, 0, 0, 0);
        Date dob = cal.getTime();
        return new DayDate(dob.getTime());
    }
}
