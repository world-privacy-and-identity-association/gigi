package club.wpia.gigi.util;

import java.util.Properties;

import club.wpia.gigi.dbObjects.CATS;

public class TimeConditions {

    private static TimeConditions instance;

    private final int testValidMonths;

    private final int reverificationDays;

    private final int verificationFreshMonths;

    private final int verificationMaxAgeMonths;

    private final int emailPingMonths;

    private TimeConditions(Properties ppts) {
        testValidMonths = Integer.parseInt(ppts.getProperty("time.testValidMonths", "12"));
        reverificationDays = Integer.parseInt(ppts.getProperty("time.reverificationDays", "90"));
        verificationFreshMonths = Integer.parseInt(ppts.getProperty("time.verificationFreshMonths", "27"));
        verificationMaxAgeMonths = Integer.parseInt(ppts.getProperty("time.verificationMaxAgeMonths", "24"));
        emailPingMonths = Integer.parseInt(ppts.getProperty("time.emailPingMonths", "6"));
    }

    public static synchronized TimeConditions getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TimeConditions class not yet initialised.");
        }
        return instance;
    }

    public static synchronized final void init(Properties ppts) {
        if (instance != null) {
            throw new IllegalStateException("TimeConditions class already initialised.");
        }
        instance = new TimeConditions(ppts);
    }

    /**
     * Maximum time in months that a passed {@link CATS} test is considered
     * recent.
     * 
     * @return the configured number of months
     */
    public int getTestMonths() {
        return testValidMonths;
    }

    /**
     * Minimum time in days that needs to have passed in order to verify a name
     * again.
     * 
     * @return the configured number of days
     */
    public int getVerificationLimitDays() {
        return reverificationDays;
    }

    /**
     * Maximum time in months that a verification is considered recent.
     * 
     * @return the configured number of months
     */
    public int getVerificationMonths() {
        return verificationFreshMonths;
    }

    /**
     * Maximum time in months that a verification can be entered after it
     * occurred. Assuming that the RA-Agent enters the correct date.
     * 
     * @return the configured number of months
     */
    public int getVerificationMaxAgeMonths() {
        return verificationMaxAgeMonths;
    }

    /**
     * Maximum time in months that an email address can be used for creating
     * client certificates before a reping is neccessary
     * 
     * @return the configured number of months
     */
    public int getEmailPingMonths() {
        return emailPingMonths;
    }
}
