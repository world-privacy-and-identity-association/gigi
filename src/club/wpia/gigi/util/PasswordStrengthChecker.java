package club.wpia.gigi.util;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.regex.Pattern;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.output.template.SprintfCommand;

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

    public static int checkpw(String pw, String[] nameParts, String email) {
        if (pw == null) {
            return 0;
        }
        int light = checkpwlight(pw);
        if (contained(pw, email)) {
            light -= 2;
        }
        for (int i = 0; i < nameParts.length; i++) {
            if (contained(pw, nameParts[i])) {
                light -= 2;
            }
        }
        // TODO dictionary check
        return light;
    }

    public static void assertStrongPassword(String pw, Name[] names, String email) throws GigiApiException {
        TreeSet<String> parts = new TreeSet<>();
        for (int i = 0; i < names.length; i++) {
            for (NamePart string : names[i].getParts()) {
                parts.add(string.getValue());
            }
        }
        if (checkpw(pw, parts.toArray(new String[parts.size()]), email) < 3) {
            throw (new GigiApiException(new SprintfCommand("The Pass Phrase you submitted failed to contain enough differing characters and/or contained words from your name and/or email address. For the current requirements and to learn more, visit our {0}FAQ{1}.", Arrays.asList("!(/wiki/goodPassword", "!'</a>'"))));
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
