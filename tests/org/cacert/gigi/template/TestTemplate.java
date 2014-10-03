package org.cacert.gigi.template;

import static org.junit.Assert.*;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Outputable;
import org.cacert.gigi.output.template.HashAlgorithms;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.OutputableArrayIterable;
import org.cacert.gigi.output.template.Template;
import org.junit.Test;

public class TestTemplate {

    private String testExecute(Language l, HashMap<String, Object> vars, String input) {
        Template t = new Template(new StringReader(input));
        CharArrayWriter caw = new CharArrayWriter();
        PrintWriter pw = new PrintWriter(caw);
        t.output(pw, l, vars);
        pw.flush();
        return new String(caw.toCharArray());
    }

    HashMap<String, Object> vars = new HashMap<>();

    @Test
    public void testVarEscape() {
        vars.put("var", "val");
        assertEquals("vall", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=$var?>l"));
        vars.put("var", "val<");
        assertEquals("val&lt;l", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=$var?>l"));
        assertEquals("val<l", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=$!var?>l"));
        vars.put("var", "val\">");
        assertEquals("val&quot;&gt;l", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=$var?>l"));
        assertEquals("val\">l", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=$!var?>l"));

    }

    @Test
    public void testVarSprintf() {
        vars.put("var", "val\">");
        vars.put("var2", "val3<\"");
        vars.put("var3", "val4>");
        assertEquals("This val&quot;&gt; textl", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=s,$var,$var2,$var3,This %s text?>l"));
        assertEquals("This val\"> textl", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=s,$!var,$!var2,$!var3,This %s text?>l"));

        assertEquals("This val&quot;&gt; val3&lt;&quot; the val4&gt; textl", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=s,$var,$var2,$var3,This %s %s the %s text?>l"));
        assertEquals("This val\"> val3<\" the val4> textl", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=s,$!var,$!var2,$!var3,This %s %s the %s text?>l"));

        assertEquals("This blargh&lt;&gt;!, <>! textl", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=s,\"blargh<>!\",!\"<>!\",This %s, %s text?>l"));
    }

    @Test
    public void testIf() {
        vars.put("existent", "val");
        assertEquals(">existent<", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<? if($existent) { ?>>existent<? } ?><<? if($nonexistent) { ?>nonexistent<? } ?>"));
    }

    @Test
    public void testForeach() {
        vars.put("it", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                vars.put("e", Integer.toString(i++));
                return i < 10;
            }
        });
        assertEquals("012345678<", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<? foreach($it) { ?><?=$e?><? } ?><<? foreach($nonexistent) { ?>nonexistent<? } ?>"));
    }

    @Test
    public void testNullContent() {
        assertEquals("<null>", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<<?=$undef?>>"));

    }

    @Test
    public void testTranslate() {
        assertEquals("&lt;null&gt;", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<?=_<null>?>"));

    }

    @Test
    public void testOutputable() {
        Outputable o = new Outputable() {

            @Override
            public void output(PrintWriter out, Language l, Map<String, Object> vars) {
                out.print("bl");
            }
        };
        vars.put("v", new OutputableArrayIterable(new Object[] {
                o, o, o, o, o
        }, "e"));
        assertEquals("[0]bl[1]bl[2]bl[3]bl[4]bl", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<? foreach($v) { ?>[<?=$i?>]<?=$e?><? } ?>"));

    }

    @Test
    public void testHashalgs() {
        vars.put("v", new HashAlgorithms(Digest.SHA256));
        assertEquals("SHA256[ checked='checked']SHA384[]SHA512[]", testExecute(Language.getInstance(Locale.ENGLISH), vars, "<? foreach($v) { ?><?=$algorithm?>[<?=$!checked?>]<? } ?>"));

    }

}