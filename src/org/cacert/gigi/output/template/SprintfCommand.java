package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Outputable;
import org.cacert.gigi.util.HTMLEncoder;

public final class SprintfCommand implements Outputable {

    private final String text;

    private final LinkedList<String> store;

    public SprintfCommand(String text, LinkedList<String> store) {
        this.text = text;
        this.store = store;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        String[] parts = l.getTranslation(text).split("%s");
        String[] myvars = store.toArray(new String[store.size()]);
        out.print(HTMLEncoder.encodeHTML(parts[0]));
        for (int j = 1; j < parts.length; j++) {
            String var = myvars[j - 1];
            if (var.startsWith("$!")) {
                Template.outputVar(out, l, vars, myvars[j - 1].substring(2), true);
            } else if (var.startsWith("\"")) {
                out.print(var.substring(1));
            } else {
                Template.outputVar(out, l, vars, myvars[j - 1].substring(1), false);
            }
            out.print(HTMLEncoder.encodeHTML(parts[j]));
        }
    }
}
