package club.wpia.gigi.output.template;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.util.HTMLEncoder;

/**
 * A pattern that is to be translated before variables are inserted.
 */
public final class SprintfCommand implements Translatable {

    /**
     * The pattern to fill. Containing placeholders of pattern
     * {@link #placeholder}. This is the string that will be translated.
     */
    private final String pattern;

    /**
     * A regex that matches the replacement patterns like "{0}" or "{1}".
     */
    private static final Pattern placeholder = Pattern.compile("\\{([0-9]+)\\}");

    /**
     * The values describing what to put into the {@link #placeholder}s of
     * {@link #pattern}.
     */
    private final String[] replacements;

    /**
     * Regex for detecting processing instructions in a in-template
     * SprintfCommand.
     */
    private static final Pattern processingInstruction = Pattern.compile("(?:(\\$!?\\{[a-zA-Z0-9_-]+)\\})|(?:(!'[^{}'\\$]*)')|(?:(!\\([^{})\\$]*)\\))");

    /**
     * Creates a new SprintfCommand based on its pre-parsed contents. This is
     * the variant that the data is stored internally in this class. So the
     * <code>pattern</code> has numbers as placeholders and the replacement list
     * contains the instructions on what to put in there (without closing
     * brackets, etc.).
     * 
     * @param pattern
     *            a string with <code>{0},{1},..</code> as placeholders.
     * @param replacements
     *            instructions for what data to put into the placeholders:
     *            <code>${var</code>, <code>$!{var</code>, <code>!'plain</code>,
     *            <code>!(/link</code>.
     */
    public SprintfCommand(String pattern, List<String> replacements) {
        this.pattern = pattern;
        this.replacements = replacements.toArray(new String[replacements.size()]);
    }

    /**
     * Creates a new SprintfCommand that is parsed as from template source. This
     * version is internally used to create {@link SprintfCommand}s from the
     * literals specified in {@link Template}s.
     * 
     * @param content
     *            the part from the template that is to be parsed.
     */
    protected SprintfCommand(String content) {
        StringBuffer pattern = new StringBuffer();
        List<String> replacements = new LinkedList<String>();
        int counter = 0;
        Matcher m = processingInstruction.matcher(content);
        int last = 0;
        while (m.find()) {
            pattern.append(content.substring(last, m.start()));
            String group = null;
            if ((group = m.group(1)) != null) {
                replacements.add(group);
            } else if ((group = m.group(2)) != null) {
                replacements.add(group);
            } else if ((group = m.group(3)) != null) {
                replacements.add(group);
            } else {
                throw new Error("Regex is broken??");
            }
            last = m.end();
            pattern.append("{" + (counter++) + "}");
        }
        pattern.append(content.substring(last));
        this.pattern = pattern.toString();
        this.replacements = replacements.toArray(new String[replacements.size()]);
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> externalVariables) {
        String parts = l.getTranslation(pattern);
        Matcher m = placeholder.matcher(parts);
        int pos = 0;
        while (m.find()) {
            out.print(escape(externalVariables, parts.substring(pos, m.start())));
            String replacement = replacements[Integer.parseInt(m.group(1))];
            if (replacement.startsWith("$!")) {
                Template.outputVar(out, l, externalVariables, replacement.substring(3), true);
            } else if (replacement.startsWith("!'")) {
                out.print(replacement.substring(2));
            } else if (replacement.startsWith("!(")) {
                String host = (String) externalVariables.get(Gigi.LINK_HOST);
                if (host == null) {
                    throw new Error("Unconfigured link-host while interpreting link-syntax.");
                }
                if (replacement.charAt(2) != '/') {
                    throw new Error("Need an absolute link for the link service.");
                }
                String link = "//" + host + replacement.substring(2);
                out.print("<a href='" + HTMLEncoder.encodeHTML(link) + "'>");
            } else if (replacement.startsWith("$")) {
                Template.outputVar(out, l, externalVariables, replacement.substring(2), false);
            } else {
                throw new Error("Processing error in template.");
            }
            pos = m.end();

        }
        out.print(escape(externalVariables, parts.substring(pos)));
    }

    private String escape(Map<String, Object> vars, String target) {
        if (vars.containsKey(OUT_KEY_PLAIN)) {
            return target;
        }
        return HTMLEncoder.encodeHTML(target);
    }

    @Override
    public void addTranslations(Collection<String> s) {
        s.add(pattern);
    }

    /**
     * Creates a simple {@link SprintfCommand} wrapped in a {@link Scope} to fit
     * in now constant variables into this template.
     * 
     * @param msg
     *            the message (to be translated) with <code>{0},{1},...</code>
     *            as placeholders.
     * @param vars
     *            the contents of the variables to put into the placeholders.
     * @return the constructed {@link Outputable}.
     */
    public static Outputable createSimple(String msg, Object... vars) {
        HashMap<String, Object> scope = new HashMap<>();
        String[] store = new String[vars.length];
        for (int i = 0; i < vars.length; i++) {
            scope.put("autoVar" + i, vars[i]);
            store[i] = "${autoVar" + i;
        }
        return new Scope(new SprintfCommand(msg, Arrays.asList(store)), scope);
    }
}
