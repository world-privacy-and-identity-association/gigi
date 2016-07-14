package org.cacert.gigi.output;

import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

public abstract class ArrayIterable<T> implements IterableDataset {

    private T[] dt;

    protected int i = 0;

    public ArrayIterable(T[] dt) {
        this.dt = dt;
    }

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        if (i >= dt.length) {
            return false;
        }
        apply(dt[i], l, vars);
        i++;
        return true;
    }

    public abstract void apply(T t, Language l, Map<String, Object> vars);

}
