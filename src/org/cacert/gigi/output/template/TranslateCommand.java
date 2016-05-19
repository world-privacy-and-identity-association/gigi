package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.HTMLEncoder;

public final class TranslateCommand implements Translatable {

    private final String raw;

    public TranslateCommand(String raw) {
        this.raw = raw;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print(HTMLEncoder.encodeHTML(l.getTranslation(raw)));
    }

    public String getRaw() {
        return raw;
    }

    @Override
    public void addTranslations(Collection<String> s) {
        s.add(raw);
    }
}
