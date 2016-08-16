package org.cacert.gigi.output;

import java.util.Iterator;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;

public abstract class IterableIterable<T> implements IterableDataset {

    private Iterator<T> dt;

    protected int i = 0;

    public IterableIterable(Iterable<T> dt) {
        this.dt = dt.iterator();
    }

    @Override
    public boolean next(Language l, Map<String, Object> vars) {
        if ( !dt.hasNext()) {
            return false;
        }
        apply(dt.next(), l, vars);
        i++;
        return true;
    }

    public abstract void apply(T t, Language l, Map<String, Object> vars);

}
