package org.cacert.gigi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestSecurityHeaders extends ManagedTest {

    @Test
    public void testSTS() throws IOException {
        HttpURLConnection uc = get(null, "/");
        assertNotNull(uc.getHeaderField("Strict-Transport-Security"));
    }

    public void testCSP() throws IOException {
        HttpURLConnection uc = get(null, "/");
        assertNotNull(uc.getHeaderField("Content-Security-Policy"));
    }

    public void testAllowOrigin() throws IOException {
        HttpURLConnection uc = get(null, "/");
        assertNotNull(uc.getHeaderField("Access-Control-Allow-Origin"));

    }
}
