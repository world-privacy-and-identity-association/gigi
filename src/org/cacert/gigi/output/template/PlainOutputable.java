package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.HTMLEncoder;

public class PlainOutputable implements Outputable {

    String text;

    public PlainOutputable(String text) {
        this.text = text;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        if (vars.containsKey(OUT_KEY_PLAIN)) {
            out.print(text);
        } else {
            out.print(HTMLEncoder.encodeHTML(text));
        }
    }

}
