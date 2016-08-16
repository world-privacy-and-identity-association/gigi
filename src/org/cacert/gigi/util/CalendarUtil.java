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

    public static boolean isOfAge(DayDate dob, int age) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dob.getTime());
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        c.set(year, month, day);
        c.add(Calendar.YEAR, age);

        return System.currentTimeMillis() >= c.getTime().getTime();
    }

    public static DayDate getDateFromComponents(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0);
        cal.set(year, month - 1, day, 0, 0, 0);
        Date dob = cal.getTime();
        return new DayDate(dob.getTime());
    }

    public static Date timeDifferenceDays(int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }
}
