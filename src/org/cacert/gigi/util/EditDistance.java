package org.cacert.gigi.util;

public class EditDistance {

    public static String getBestMatchingStringByEditDistance(String needle, Iterable<String> possibleStrings) {
        String best = "";
        int bestDistance = Integer.MAX_VALUE;
        for (String possibleString : possibleStrings) {
            int newDistance = calculateLevenshteinDistance(possibleString, needle);
            if (newDistance < bestDistance) {
                bestDistance = newDistance;
                best = possibleString;
            }
        }
        return best;
    }

    public static String getBestMatchingStringByEditDistance(String needle, String[] possibleStrings) {
        if (possibleStrings.length == 0) {
            return "";
        }
        String best = possibleStrings[0];
        int bestDistance = Integer.MAX_VALUE;
        for (String possibleString : possibleStrings) {
            int newDistance = calculateLevenshteinDistance(possibleString, needle);
            if (newDistance < bestDistance) {
                bestDistance = newDistance;
                best = possibleString;
            }
        }
        return best;
    }

    /**
     * Calculates the levenshtein edit distance between the passed strings.
     * Adapted from https://en.wikipedia.org/wiki/Levenshtein_distance
     */
    public static int calculateLevenshteinDistance(String s, String t) {
        // degenerate cases
        if (s == t || s.equals(t)) {
            return 0;
        }
        if (s.length() == 0) {
            return t.length();
        }
        if (t.length() == 0) {
            return s.length();
        }

        // create two work arrays of integer distances
        int[] previousRow = new int[t.length() + 1];
        int[] currentRow = new int[t.length() + 1];

        // initialize previousRow
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < previousRow.length; i++) {
            previousRow[i] = i;
        }

        for (int i = 0; i < s.length(); i++) {
            // calculate current row from the previous row

            // first element of currentRow is A[i+1][0]
            // edit distance is delete (i+1) chars from s to match empty t
            currentRow[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < t.length(); j++) {
                int cost = s.charAt(i) == t.charAt(j) ? 0 : 1;
                currentRow[j + 1] = Math.min(Math.min(currentRow[j] + 1, previousRow[j + 1] + 1), previousRow[j] + cost);
            }

            System.arraycopy(currentRow, 0, previousRow, 0, currentRow.length);
        }

        return currentRow[t.length()];
    }
}
