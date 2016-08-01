package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.dbObjects.Assurance;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.Template;

public class AssurancesDisplay implements Outputable {

    private static final Template template = new Template(AssurancesDisplay.class.getResource("AssurancesDisplay.templ"));

    private boolean assurer;

    public String assuranceArray;

    public AssurancesDisplay(String assuranceArray, boolean assurer) {
        this.assuranceArray = assuranceArray;
        this.assurer = assurer;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        final Assurance[] assurances = (Assurance[]) vars.get(assuranceArray);
        if (assurer) {
            vars.put("verb", l.getTranslation("To (User Id)"));
        } else {
            vars.put("verb", l.getTranslation("From"));
            vars.put("myName", "yes");
        }

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
                    Name to = assurance.getTo();
                    if (assurer) {
                        vars.put("verbVal", to == null ? l.getTranslation("applicant's name removed") : to.getOwner().getId());
                        vars.put("myName", to == null ? l.getTranslation("applicant's name removed") : to);
                    } else {
                        vars.put("verbVal", assurance.getFrom().getPreferredName());
                        vars.put("myName", to == null ? l.getTranslation("own name removed") : to);
                    }
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
