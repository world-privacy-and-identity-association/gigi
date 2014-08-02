package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Outputable;

public final class OutputVariableCommand implements Outputable {

    private final String raw;

    private final boolean escaped;

    public OutputVariableCommand(String raw) {
        if (raw.charAt(0) == '!') {
            escaped = true;
            this.raw = raw.substring(1);
        } else {
            escaped = true;
            this.raw = raw;
        }
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        Template.outputVar(out, l, vars, raw, escaped);
    }
}
