package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.HTMLEncoder;

public final class SprintfCommand implements Translatable {

    private final String text;

    private final String[] store;

    public SprintfCommand(String text, List<String> store) {
        this.text = text;
        this.store = store.toArray(new String[store.size()]);
    }

    private static final String VARIABLE = "\\$!?\\{[a-zA-Z0-9_-]+\\}";

    private static final Pattern processingInstruction = Pattern.compile("(" + VARIABLE + ")|(!'[^{}'\\$]*)'");

    public SprintfCommand(String content) {
        StringBuffer raw = new StringBuffer();
        List<String> var = new LinkedList<String>();
        int counter = 0;
        Matcher m = processingInstruction.matcher(content);
        int last = 0;
        while (m.find()) {
            raw.append(content.substring(last, m.start()));
            String group = null;
            if ((group = m.group(1)) != null) {
                var.add(group);
            } else if ((group = m.group(2)) != null) {
                var.add(group);
            } else {
                throw new Error("Regex is broken??");
            }
            last = m.end();
            raw.append("{" + (counter++) + "}");
        }
        raw.append(content.substring(last));
        text = raw.toString();
        store = var.toArray(new String[var.size()]);
    }

    private final Pattern replacant = Pattern.compile("\\{([0-9]+)\\}");

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        String parts = l.getTranslation(text);
        Matcher m = replacant.matcher(parts);
        int pos = 0;
        while (m.find()) {
            out.print(HTMLEncoder.encodeHTML(parts.substring(pos, m.start())));
            String var = store[Integer.parseInt(m.group(1))];
            if (var.startsWith("$!")) {
                Template.outputVar(out, l, vars, var.substring(3, var.length() - 1), true);
            } else if (var.startsWith("!'")) {
                out.print(var.substring(2));
            } else if (var.startsWith("$")) {
                Template.outputVar(out, l, vars, var.substring(2, var.length() - 1), false);
            } else {
                throw new Error("Processing error in template.");
            }
            pos = m.end();

        }
        out.print(HTMLEncoder.encodeHTML(parts.substring(pos)));
    }

    @Override
    public void addTranslations(Collection<String> s) {
        s.add(text);
    }
}
