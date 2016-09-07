package org.cacert.gigi.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class EditDistanceTest {

    private static final String[] words = new String[] {
            "A", "B", "C", "aa", "bb", "cc c", "dfdf", "__*"
    };

    private static final List<String> wordList = Arrays.asList(words);

    @Parameters(name = "next({0}) = {1}")
    public static Iterable<Object[]> params() {
        List<Object[]> testVectors = new LinkedList<Object[]>();
        for (String word : words) {
            addTest(testVectors, word, word);
            addTest(testVectors, word + " ", word);
            addTest(testVectors, word + "_", word);
            addTest(testVectors, " " + word, word);
            addTest(testVectors, "werwer" + word, word);
        }
        addTest(testVectors, "Acc c", "cc c");
        addTest(testVectors, "bbB", "bb");
        addTest(testVectors, "dfÄö", "dfdf");
        addTest(testVectors, "*__", "__*");
        addTest(testVectors, "c cc", "cc c");
        addTest(testVectors, "fddf", "dfdf");
        addTest(testVectors, "fdd..", "dfdf");
        addTest(testVectors, "*", "A");
        addTest(testVectors, "A__*", "__*");
        return testVectors;
    }

    private static void addTest(List<Object[]> testVectors, String test, String expected) {
        testVectors.add(new Object[] {
                test, expected
        });
    }

    @Parameter(0)
    public String search;

    @Parameter(1)
    public String expected;

    @Test
    public void getBestMatchingStringByEditDistanceArray() {
        assertEquals(expected, EditDistance.getBestMatchingStringByEditDistance(search, words));
    }

    @Test
    public void getBestMatchingStringByEditDistanceList() {
        assertEquals(expected, EditDistance.getBestMatchingStringByEditDistance(search, wordList));
    }

}
