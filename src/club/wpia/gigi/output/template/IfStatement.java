package club.wpia.gigi.output.template;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import club.wpia.gigi.localisation.Language;

/**
 * One ore two {@link Outputable}s that are emitted conditionally if a given
 * variable is neither <code>null</code> nor {@link Boolean#FALSE}.
 */
public final class IfStatement implements Translatable {

    private final String variable;

    private final TemplateBlock iftrue;

    private final TemplateBlock iffalse;

    /**
     * Creates a new {@link IfStatement} with an empty else-part.
     * 
     * @param variable
     *            the variable to check.
     * @param body
     *            the body to emit conditionally.
     */
    public IfStatement(String variable, TemplateBlock body) {
        this.variable = variable;
        this.iftrue = body;
        this.iffalse = null;
    }

    /**
     * Creates a new {@link IfStatement} with an else-block.
     * 
     * @param variable
     *            the variable to check.
     * @param iftrue
     *            the block to emit if the check succeeds.
     * @param iffalse
     *            the block to emit if the check fails.
     */
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

    @Override
    public void addTranslations(Collection<String> s) {
        iftrue.addTranslations(s);
        if (iffalse != null) {
            iffalse.addTranslations(s);
        }
    }

}
