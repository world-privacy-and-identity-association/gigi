package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.HTMLEncoder;

/**
 * Wraps a String that needs to be translated before it is printed to the user.
 */
public final class TranslateCommand implements Translatable {

    private final String raw;

    /**
     * Creates a new TranslateCommand that wraps the given String.
     * 
     * @param raw
     *            the String to be translated.
     */
    public TranslateCommand(String raw) {
        this.raw = raw;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print(HTMLEncoder.encodeHTML(l.getTranslation(raw)));
    }

    /**
     * Gets the raw, untranslated String.
     * 
     * @return the raw, untranslated String.
     */
    public String getRaw() {
        return raw;
    }

    @Override
    public void addTranslations(Collection<String> s) {
        s.add(raw);
    }
}
