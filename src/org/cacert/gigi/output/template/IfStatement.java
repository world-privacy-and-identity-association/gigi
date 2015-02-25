package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;

public final class IfStatement implements Outputable {

    private final String variable;

    private final TemplateBlock iftrue;

    private final TemplateBlock iffalse;

    public IfStatement(String variable, TemplateBlock body) {
        this.variable = variable;
        this.iftrue = body;
        this.iffalse = null;
    }

    public IfStatement(String variable, TemplateBlock iftrue, TemplateBlock iffalse) {
        this.variable = variable;
        this.iftrue = iftrue;
        this.iffalse = iffalse;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        Object o = vars.get(variable);

        if ( !(o == null || Boolean.FALSE.equals(o))) {
            iftrue.output(out, l, vars);
        } else if (iffalse != null) {
            iffalse.output(out, l, vars);
        }
    }

}
