package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import club.wpia.gigi.dbObjects.Assurance;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.Template;

public class AssurancesDisplay implements Outputable {

    private static final Template template = new Template(AssurancesDisplay.class.getResource("AssurancesDisplay.templ"));

    private boolean assurer;

    public String assuranceArray;

    private boolean support;

    public AssurancesDisplay(String assuranceArray, boolean assurer, boolean support) {
        this.assuranceArray = assuranceArray;
        this.assurer = assurer;
        this.support = support;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        final Assurance[] assurances = (Assurance[]) vars.get(assuranceArray);
        if (assurer) {
            vars.put("verb", l.getTranslation("To (User Id)"));
            vars.put("info", "");
        } else {
            vars.put("verb", l.getTranslation("From"));
            vars.put("myName", "yes");
            vars.put("info", l.getTranslation("Coloured rows show expired nucleus bonus verifications which are not counted to the total of verification points."));
        }

        IterableDataset assuranceGroup = new IterableDataset() {

            private int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i >= assurances.length) {
                    return false;
                } else {
                    Assurance assurance = assurances[i];
                    vars.put("support", support);
                    vars.put("id", assurance.getId());
                    vars.put("method", assurance.getMethod());
                    Name to = assurance.getTo();
                    if (assurer) {
                        vars.put("linkId", to == null ? "" : to.getOwner().getId());
                        vars.put("verbVal", to == null ? l.getTranslation("applicant's name removed") : to.getOwner().getId());
                        vars.put("myName", to == null ? l.getTranslation("applicant's name removed") : to);
                    } else {
                        vars.put("linkId", assurance.getFrom().getId());
                        vars.put("verbVal", assurance.getFrom().getPreferredName());
                        vars.put("myName", to == null ? l.getTranslation("own name removed") : to);
                    }
                    vars.put("date", assurance.getDate());
                    vars.put("location", assurance.getLocation() + " (" + (assurance.getCountry() == null ? l.getTranslation("not given") : assurance.getCountry().getName()) + ")");
                    vars.put("points", assurance.getPoints());
                    vars.put("expired", assurance.isExpired());
                    i++;
                    return true;
                }
            }
        };
        vars.put("assurances", assuranceGroup);
        template.output(out, l, vars);
    }

}
