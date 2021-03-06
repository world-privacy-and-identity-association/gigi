package club.wpia.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import club.wpia.gigi.localisation.Language;

/**
 * An object that is outputable to the user normally in an HTML-page.
 */
public interface Outputable {

    public static final String OUT_KEY_PLAIN = "output-content-plain";

    /**
     * Writes this object's content to the given output stream.
     * 
     * @param out
     *            the PrintWriter to the user.
     * @param l
     *            the {@link Language} to translate localizable strings to.
     * @param vars
     *            a map of variable assignments for this template.
     */
    public void output(PrintWriter out, Language l, Map<String, Object> vars);
}
