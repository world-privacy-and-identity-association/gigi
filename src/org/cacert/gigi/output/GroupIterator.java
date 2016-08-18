package org.cacert.gigi.output;

import java.util.Iterator;
import java.util.Map;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

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
