package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.HTMLEncoder;

public class PlainOutputable implements Outputable {

    String text;

    public PlainOutputable(String text) {
        this.text = HTMLEncoder.encodeHTML(text);
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print(text);
    }

}
