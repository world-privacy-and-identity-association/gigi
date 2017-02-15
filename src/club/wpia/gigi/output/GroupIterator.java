package club.wpia.gigi.output;

import java.util.Iterator;
import java.util.Map;

import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.IterableDataset;

public class GroupIterator implements IterableDataset {

    private final boolean asSupport;

    private final Iterator<Group> i;

    public GroupIterator(Iterator<Group> i, boolean asSupport) {
        this.asSupport = asSupport;
        this.i = i;
    }

    private int j = 0;

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        while (i.hasNext()) {
            Group g = i.next();
            if (g.isManagedBySupport() == asSupport) {
                vars.put("group_concat", (j > 0 ? ", " : ""));
                vars.put("group", g.getName());
                j++;
                return true;
            }
        }

        return false;
    }
}
