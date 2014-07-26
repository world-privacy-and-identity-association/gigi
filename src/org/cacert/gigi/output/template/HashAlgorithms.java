package org.cacert.gigi.output.template;

import java.util.Map;

import org.cacert.gigi.Digest;
import org.cacert.gigi.Language;

public class HashAlgorithms implements IterableDataset {

    int i = 0;

    Digest selected;

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
        vars.put("info", l.getTranslation(d.getExp()));
        vars.put("checked", selected == d ? " checked='checked'" : "");
        return true;
    }
}
