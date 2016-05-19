package org.cacert.gigi.output;

import java.util.Map;

import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

public class HashAlgorithms implements IterableDataset {

    private int i = 0;

    private Digest selected;

    public HashAlgorithms(Digest selected) {
        this.selected = selected;
    }

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        Digest[] length = Digest.values();
        if (i >= length.length) {
            return false;
        }
        Digest d = length[i++];
        vars.put("algorithm", d.toString());
        vars.put("name", d.toString());
        vars.put("info", d.getExp());
        vars.put("checked", selected == d ? " checked='checked'" : "");
        return true;
    }
}
