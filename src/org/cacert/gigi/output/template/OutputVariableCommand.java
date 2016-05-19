package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import org.cacert.gigi.localisation.Language;

public final class OutputVariableCommand implements Translatable {

    private final String raw;

    private final boolean unescaped;

    public OutputVariableCommand(String raw) {
        if (raw.charAt(0) == '!') {
            unescaped = true;
            this.raw = raw.substring(1);
        } else {
            unescaped = false;
            this.raw = raw;
        }
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        Template.outputVar(out, l, vars, raw, unescaped);
    }

    @Override
    public void addTranslations(Collection<String> s) {}
}
