package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import club.wpia.gigi.GigiApiException;

@RunWith(Parameterized.class)
public class TestSerialNormalization {

    private final String input;

    private final String normalized;

    @Parameters
    public static List<String[]> params() {
        return Arrays.asList(new String[] {
                "dead00beef", "dead00beef"
        }, new String[] {
                "Dead00beef", "dead00beef"
        }, new String[] {
                "DEAD00BEEF", "dead00beef"
        }, new String[] {
                "00DEAD00BEEF", "dead00beef"
        }, new String[] {
                " 00dead00beef", "dead00beef"
        }, new String[] {
                "00dead00beef ", "dead00beef"
        }, new String[] {
                " 00dead00beef ", "dead00beef"
        }, new String[] {
                " 00dead 00beef ", "dead00beef"
        }, new String[] {
                " 00d ead 00beef ", "dead00beef"
        }, new String[] {
                "de:ad:00:be:ef", "dead00beef"
        }, new String[] {
                "00:de:ad:03:be:ef", "dead03beef"
        }, new String[] {
                "08:15:47:11", "8154711"
        }, new String[] {
                " 00:de:Ad:43:be:ef ", "dead43beef"
        }, new String[] {
                "00:de:ad:43:beef", null
        }, new String[] {
                "g", null
        }, new String[] {
                ".", null
        });
    }

    public TestSerialNormalization(String input, String normalized) {
        this.input = input;
        this.normalized = normalized;
    }

    @Test
    public void testNormalize() throws GigiApiException {
        if (normalized == null) {
            try {
                Certificate.normalizeSerial(input);
                fail("malformed serial accepted");
            } catch (GigiApiException e) {
                return;
            }
        }
        assertEquals(normalized, Certificate.normalizeSerial(input));
    }
}
