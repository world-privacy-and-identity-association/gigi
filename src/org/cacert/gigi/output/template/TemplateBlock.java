package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Outputable;

class TemplateBlock implements Outputable {

    String[] contents;

    Outputable[] vars;

    public TemplateBlock(String[] contents, Outputable[] vars) {
        this.contents = contents;
        this.vars = vars;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        for (int i = 0; i < contents.length; i++) {
            out.print(contents[i]);
            if (i < this.vars.length) {
                this.vars[i].output(out, l, vars);
            }
        }
    }

}
