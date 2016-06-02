package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.cacert.gigi.localisation.Language;

/**
 * Builds a variable scope around another {@link Outputable}, statically filling
 * variables.
 */
public class Scope implements Outputable {

    private Map<String, Object> vars;

    private Outputable out;

    /**
     * Creates a new {@link Scope}.
     * 
     * @param out
     *            the enclosed {@link Outputable}.
     * @param vars
     *            the variables to assign in the inner scope.
     */
    public Scope(Outputable out, Map<String, Object> vars) {
        this.out = out;
        this.vars = vars;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        HashMap<String, Object> map = new HashMap<>();
        map.putAll(vars);
        map.putAll(this.vars);
        this.out.output(out, l, map);
    }

}
