package club.wpia.gigi.util;

import java.sql.Date;

/**
 * This Class consists of a millisecond timestamp that is only interesting up to
 * day-precision.
 */
public class DayDate {

    private static final int MILLI_HOUR = 60 * 60 * 1000;

    public static final long MILLI_DAY = 24 * MILLI_HOUR;

    private long time;

    /**
     * Creates a new {@link DayDate} from the SQL Day-exact variant {@link Date}
     * .
     * 
     * @see #toSQLDate()
     */
    public DayDate(Date date) {
        this(date.getTime());
    }

    /**
     * Creates a new {@link DayDate} based on the given millisecond timestamp.
     * 
     * @param millis
     *            the timestamp to create the Date from.
     * @throws IllegalArgumentException
     *             if the parameter contains more precision than needed.
     */
    public DayDate(long millis) {
        this.time = millis;
        if (millis % MILLI_DAY != 0) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the enclosed timestamp.
     * 
     * @return the enclosed timestamp.
     */
    public long getTime() {
        return time;
    }

    /**
     * Converts this DayDate to an {@link Date}.
     * 
     * @return the corresponding {@link Date}
     * @see #DayDate(Date)
     */
    public Date toSQLDate() {
        return new Date(time);
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if ( !(obj instanceof DayDate)) {
            throw new Error("You may not compare this date somthing other than a DayDate");
        }
        return ((DayDate) obj).time == time;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(time);
    }

    public java.util.Date toDate() {
        return new java.util.Date(time);
    }

    // Timezones currently reach from UTC-12 to UTC+14 that allows us to define
    // the "earliest start" and "latest end" of a Date.
    /**
     * Gets this date's starting point.
     * 
     * @return The earliest point in time where this date was in any timezone
     */
    public java.util.Date start() {
        return new java.util.Date(time - 12 * MILLI_HOUR);
    }

    /**
     * Gets this date's ending point.
     * 
     * @return The latest point in time where this Date was any timezone
     */
    public java.util.Date end() {
        return new java.util.Date(time + 14 * MILLI_HOUR + 24 * MILLI_HOUR);
    }
}
