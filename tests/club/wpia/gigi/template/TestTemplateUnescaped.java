package club.wpia.gigi.template;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;

import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.Template;

public class TestTemplateUnescaped {

    private String testExecute(HashMap<String, Object> vars, String input) {
        Template t = new Template(new StringReader(input));
        StringWriter str = new StringWriter();
        PrintWriter pw = new PrintWriter(str);
        t.output(pw, Language.getInstance(Locale.ENGLISH), vars);
        pw.flush();
        return str.toString();
    }

    HashMap<String, Object> vars = new HashMap<>(Collections.<String, Object>singletonMap(Outputable.OUT_KEY_PLAIN, "yes"));

    @Test
    public void testVarNoEscape() {
        vars.put("var", "val");
        assertEquals("vall", testExecute(vars, "<?=$var?>l"));
        vars.put("var", "val<");
        assertEquals("val<l", testExecute(vars, "<?=$var?>l"));
        assertEquals("val<l", testExecute(vars, "<?=$!var?>l"));
        vars.put("var", "val\">");
        assertEquals("val\">l", testExecute(vars, "<?=$var?>l"));
        assertEquals("val\">l", testExecute(vars, "<?=$!var?>l"));
    }

    @Test
    public void testTranslateNoEscape() {
        assertEquals("\"tex<>l", testExecute(vars, "<?=_\"tex<>?>l"));
    }
}
