package org.cacert.gigi.util;

import java.util.regex.Pattern;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.User;

public class PasswordStrengthChecker {

    private static Pattern digits = Pattern.compile("\\d");

    private static Pattern lower = Pattern.compile("[a-z]");

    private static Pattern upper = Pattern.compile("[A-Z]");

    private static Pattern whitespace = Pattern.compile("\\s");

    private static Pattern special = Pattern.compile("(?!\\s)\\W");

    private PasswordStrengthChecker() {}

    private static int checkpwlight(String pw) {
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

    public static int checkpw(String pw, User u) {
        if (pw == null) {
            return 0;
        }
        int light = checkpwlight(pw);
        if (contained(pw, u.getEmail())) {
            light -= 2;
        }
        if (contained(pw, u.getFname())) {
            light -= 2;
        }
        if (contained(pw, u.getLname())) {
            light -= 2;
        }
        if (contained(pw, u.getMname())) {
            light -= 2;
        }
        if (contained(pw, u.getSuffix())) {
            light -= 2;
        }
        // TODO dictionary check
        return light;
    }

    public static void assertStrongPassword(String pw, User u) throws GigiApiException {
        if (checkpw(pw, u) < 3) {
            throw new GigiApiException("The Pass Phrase you submitted failed to contain enough" + " differing characters and/or contained words from" + " your name and/or email address.");
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
