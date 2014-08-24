package org.cacert.gigi.output.template;

import java.util.Map;

import org.cacert.gigi.localisation.Language;

public class OutputableArrayIterable implements IterableDataset {

    Object[] content;

    String targetName;

    int index = 0;

    public OutputableArrayIterable(Object[] content, String targetName) {
        this.content = content;
        this.targetName = targetName;
    }

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        if (index >= content.length) {
            return false;
        }
        vars.put(targetName, content[index]);
        vars.put("i", index);
        index++;
        return true;
    }

}
