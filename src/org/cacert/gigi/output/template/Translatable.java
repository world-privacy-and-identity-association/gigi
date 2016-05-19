package org.cacert.gigi.output.template;

import java.util.Collection;

public interface Translatable extends Outputable {

    public void addTranslations(Collection<String> s);
}
