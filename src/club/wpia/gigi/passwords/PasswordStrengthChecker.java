package club.wpia.gigi.passwords;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.regex.Pattern;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.output.template.SprintfCommand;

public class PasswordStrengthChecker implements PasswordChecker {

    private static Pattern digits = Pattern.compile("\\d");

    private static Pattern lower = Pattern.compile("[a-z]");

    private static Pattern upper = Pattern.compile("[A-Z]");

    private static Pattern whitespace = Pattern.compile("\\s");

    private static Pattern special = Pattern.compile("(?!\\s)\\W");

    public PasswordStrengthChecker() {}

    /**
     * @param pw The password.
     * @return Estimate of the password’s strength (positive).
     */
    private int ratePasswordStrength(String pw) {
        int points = 0;
        if (pw.length() > 15) {
            points++;
        }
        if (pw.length() > 20) {
            points++;
        }
        if (pw.length() > 25) {
            points++;
        }
        if (pw.length() > 30) {
            points++;
        }
        if (digits.matcher(pw).find()) {
            points++;
        }
        if (lower.matcher(pw).find()) {
            points++;
        }
        if (upper.matcher(pw).find()) {
            points++;
        }
        if (special.matcher(pw).find()) {
            points++;
        }
        if (whitespace.matcher(pw).find()) {
            points++;
        }
        return points;
    }

    /**
     * @param pw The password.
     * @param nameParts The name parts of the user.
     * @param email The email address of the user.
     * @return Estimate of the password’s weakness (negative).
     */
    private int ratePasswordWeakness(String pw, String[] nameParts, String email) {
        int points = 0;
        if (contained(pw, email)) {
            points -= 2;
        }
        for (int i = 0; i < nameParts.length; i++) {
            if (contained(pw, nameParts[i])) {
                points -= 2;
            }
        }
        return points;
    }

    public int ratePassword(String pw, String[] nameParts, String email) {
        return ratePasswordStrength(pw) + ratePasswordWeakness(pw, nameParts, email);
    }

    @Override
    public GigiApiException checkPassword(String password, String[] nameParts, String email) {
        int points = ratePassword(password, nameParts, email);
        if (points < 3) {
            return new GigiApiException(new SprintfCommand(
                "The Password you submitted failed to contain enough differing characters and/or contained words from your name and/or email address. For the current requirements and to learn more, visit our {0}FAQ{1}.",
                Arrays.asList("!(/kb/goodPassword", "!'</a>'")
            ));
        } else {
            return null;
        }
    }

    private static boolean contained(String pw, String check) {
        if (check == null || check.equals("")) {
            return false;
        }
        if (pw.contains(check)) {
            return true;
        }
        if (check.contains(pw)) {
            return true;
        }
        return false;
    }

}
