package org.cacert.gigi.util;

import java.util.regex.Pattern;

import org.cacert.gigi.User;

public class PasswordStrengthChecker {
	static Pattern digits = Pattern.compile("\\d");
	static Pattern lower = Pattern.compile("[a-z]");
	static Pattern upper = Pattern.compile("[A-Z]");
	static Pattern whitespace = Pattern.compile("\\s");
	static Pattern special = Pattern.compile("\\W");
	private PasswordStrengthChecker() {
	}
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
