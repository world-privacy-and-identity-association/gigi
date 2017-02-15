package club.wpia.gigi.output.template;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import club.wpia.gigi.localisation.Language;

/**
 * Outputs an {@link Outputable} multiple times based on a given
 * {@link IterableDataset}.
 */
public final class ForeachStatement implements Translatable {

    private final String variable;

    private final TemplateBlock body;

    /**
     * Creates a new {@link ForeachStatement}.
     * 
     * @param variable
     *            the variable to take the {@link IterableDataset} from.
     * @param body
     *            the body to output multiple times.
     */
    public ForeachStatement(String variable, TemplateBlock body) {
        this.variable = variable;
        this.body = body;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        Object o = vars.get(variable);
        if (o instanceof IterableDataset) {
            IterableDataset id = (IterableDataset) o;
            Map<String, Object> subcontext = new HashMap<String, Object>(vars);
            while (id.next(l, subcontext)) {
                body.output(out, l, subcontext);
            }
        }
    }

    @Override
    public void addTranslations(Collection<String> s) {
        body.addTranslations(s);
    }
}
