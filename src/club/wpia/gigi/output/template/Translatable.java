package club.wpia.gigi.output.template;

import java.util.Collection;

/**
 * An {@link Outputable} that wants to give static strings to translation
 * collection.
 */
public interface Translatable extends Outputable {

    /**
     * Adds all static translation Strings to the given {@link Collection}.
     * 
     * @param s
     *            the {@link Collection} to add the Strings to.
     */
    public void addTranslations(Collection<String> s);
}
