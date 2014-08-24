package org.cacert.gigi.output;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.Assurance;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;

public class AssurancesDisplay implements Outputable {

    private static Template template;

    public String assuranceArray;

    static {
        template = new Template(new InputStreamReader(AssurancesDisplay.class.getResourceAsStream("AssurancesDisplay.templ")));
    }

    public AssurancesDisplay(String assuranceArray) {
        this.assuranceArray = assuranceArray;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        final Assurance[] assurances = (Assurance[]) vars.get(assuranceArray);
        IterableDataset assuranceGroup = new IterableDataset() {

            private int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i >= assurances.length) {
                    return false;
                } else {
                    Assurance assurance = assurances[i];
                    vars.put("id", assurance.getId());
                    vars.put("method", assurance.getMethod());
                    vars.put("from", assurance.getFrom().getName());
                    vars.put("to", assurance.getTo().getName());
                    vars.put("date", assurance.getDate());
                    vars.put("location", assurance.getLocation());
                    vars.put("points", assurance.getPoints());
                    i++;
                    return true;
                }
            }
        };
        vars.put("assurances", assuranceGroup);
        template.output(out, l, vars);
    }

}
