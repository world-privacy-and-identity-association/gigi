package club.wpia.gigi.output.template;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import club.wpia.gigi.localisation.Language;

class TemplateBlock implements Translatable {

    private String[] contents;

    private Translatable[] vars;

    public TemplateBlock(String[] contents, Translatable[] vars) {
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

    public void addTranslations(Collection<String> s) {
        for (Translatable t : vars) {
            t.addTranslations(s);
        }
    }

}
