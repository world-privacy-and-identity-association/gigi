package org.cacert.gigi.output.template;

import java.util.Map;

import org.cacert.gigi.localisation.Language;

/**
 * Generic implementation of {@link IterableDataset} that is fed by an array.
 */
public class OutputableArrayIterable implements IterableDataset {

    private Object[] content;

    private String targetName;

    private int index = 0;

    /**
     * Creates a new {@link OutputableArrayIterable}.
     * 
     * @param content
     *            the objects to be iterated over.
     * @param targetName
     *            the variable where the contents of the array to be put in the
     *            loop.
     */
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
