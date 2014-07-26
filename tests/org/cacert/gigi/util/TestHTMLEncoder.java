package org.cacert.gigi.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestHTMLEncoder {

    @Test
    public void testEncodeSimpleString() {
        assertEquals("1234_ä", HTMLEncoder.encodeHTML("1234_ä"));
    }

    @Test
    public void testEncodeQuotes() {
        assertEquals("\\&quot;_ä.", HTMLEncoder.encodeHTML("\\\"_ä."));
    }

    @Test
    public void testEncodeTagString() {
        assertEquals("&lt;td class=&quot;&amp;amp;&quot;&gt;", HTMLEncoder.encodeHTML("<td class=\"&amp;\">"));
    }

    @Test
    public void testEncodeSingleQuoteString() {
        assertEquals("&#39;&amp;#39;", HTMLEncoder.encodeHTML("'&#39;"));
    }
}
