package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Outputable;

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
        out.print(parts[0]);
        for (int j = 1; j < parts.length; j++) {
            Template.outputVar(out, l, vars, myvars[j - 1].substring(1));
            out.print(parts[j]);
        }
    }
}
