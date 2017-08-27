package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.Verification;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.Template;

public class VerificationsDisplay implements Outputable {

    private static final Template template = new Template(VerificationsDisplay.class.getResource("VerificationsDisplay.templ"));

    private boolean agent;

    public String verificationArray;

    private boolean support;

    public VerificationsDisplay(String verificationArray, boolean agent, boolean support) {
        this.verificationArray = verificationArray;
        this.agent = agent;
        this.support = support;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        final Verification[] verifications = (Verification[]) vars.get(verificationArray);
        if (agent) {
            vars.put("verb", l.getTranslation("To (User Id)"));
            vars.put("info", "");
        } else {
            vars.put("verb", l.getTranslation("From"));
            vars.put("myName", "yes");
            vars.put("info", l.getTranslation("Coloured rows show expired nucleus bonus verifications which are not counted to the total of verification points."));
        }

        IterableDataset verificationsGroup = new IterableDataset() {

            private int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i >= verifications.length) {
                    return false;
                } else {
                    Verification verification = verifications[i];
                    vars.put("support", support);
                    vars.put("id", verification.getId());
                    vars.put("method", verification.getMethod());
                    Name to = verification.getTo();
                    if (agent) {
                        vars.put("linkId", to == null ? "" : to.getOwner().getId());
                        vars.put("verbVal", to == null ? l.getTranslation("applicant's name removed") : to.getOwner().getId());
                        vars.put("myName", to == null ? l.getTranslation("applicant's name removed") : to);
                        vars.put("agentUnverified", false);
                    } else {
                        vars.put("linkId", verification.getFrom().getId());
                        Name name = verification.getFrom().getPreferredName();
                        vars.put("verbVal", name);
                        vars.put("myName", to == null ? l.getTranslation("own name removed") : to);
                        vars.put("agentUnverified", name.getVerificationPoints() <= 0);
                    }
                    vars.put("date", verification.getDate());
                    vars.put("location", verification.getLocation() + " (" + (verification.getCountry() == null ? l.getTranslation("not given") : verification.getCountry().getName()) + ")");
                    vars.put("points", verification.getPoints());
                    vars.put("expired", verification.isExpired());
                    i++;
                    return true;
                }
            }
        };
        vars.put("verifications", verificationsGroup);
        template.output(out, l, vars);
    }

}
