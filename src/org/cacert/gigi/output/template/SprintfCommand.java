package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.HTMLEncoder;

/**
 * A pattern that is to be translated before variables are inserted.
 */
public final class SprintfCommand implements Translatable {

    private final String text;

    private final String[] store;

    /**
     * Creates a new SprintfCommand based on its pre-parsed contents
     * 
     * @param text
     *            a string with <code>{0},{1},..</code> as placeholders.
     * @param store
     *            the data to put into the placeholders: ${var}, $!{var},
     *            !'plain'.
     */
    public SprintfCommand(String text, List<String> store) {
        this.text = text;
        this.store = store.toArray(new String[store.size()]);
    }

    private static final String VARIABLE = "\\$!?\\{[a-zA-Z0-9_-]+\\}";

    private static final Pattern processingInstruction = Pattern.compile("(" + VARIABLE + ")|(!'[^{}'\\$]*)'");

    /**
     * Creates a new SprintfCommand that is parsed as from template source.
     * 
     * @param content
     *            the part from the template that is to be parsed
     */
    protected SprintfCommand(String content) {
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

    private static final Pattern replacant = Pattern.compile("\\{([0-9]+)\\}");

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

    /**
     * Creates a simple {@link SprintfCommand} wrapped in a {@link Scope} to fit
     * in now constant variables into this template.
     * 
     * @param msg
     *            the message (to be translated) with <code>{0},{1},...</code>
     *            as placeholders.
     * @param vars
     *            the variables to put into the placeholders.
     * @return the constructed {@link Outputable}
     */
    public static Outputable createSimple(String msg, String... vars) {
        HashMap<String, Object> scope = new HashMap<>();
        String[] store = new String[vars.length];
        for (int i = 0; i < vars.length; i++) {
            scope.put("autoVar" + i, vars[i]);
            store[i] = "${autoVar" + i + "}";
        }
        return new Scope(new SprintfCommand(msg, Arrays.asList(store)), scope);
    }
}
