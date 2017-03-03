package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;

public class GroupList implements Outputable {

    private final Iterable<Group> groups;

    private final boolean supportGroups;

    public GroupList(Iterable<Group> groups, boolean supportGroups) {
        this.groups = groups;
        this.supportGroups = supportGroups;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        boolean fst = true;
        for (Group g : groups) {
            if (g.isManagedBySupport() != supportGroups) {
                continue;
            }
            if ( !fst) {
                out.write(", ");
            } else {
                fst = false;
            }
            g.getName().output(out, l, vars);
        }
        if (fst) {
            out.println(l.getTranslation("no entries"));
        }
    }

}
