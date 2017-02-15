package club.wpia.gigi.output.template;

import java.util.Map;

import club.wpia.gigi.localisation.Language;

/**
 * Represents some kind of data, that may be iterated over in a template using
 * the <code>foreach</code> statement.
 */
public interface IterableDataset {

    /**
     * Moves to the next Dataset.
     * 
     * @param l
     *            the language for l10n-ed strings.
     * @param vars
     *            the variables used in this template. They need to be updated
     *            for each line.
     * @return true, iff there was a data-line "installed". False of this set is
     *         already empty.
     */
    public boolean next(Language l, Map<String, Object> vars);
}
