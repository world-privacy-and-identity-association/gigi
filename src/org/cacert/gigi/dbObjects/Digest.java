package org.cacert.gigi.dbObjects;

import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.TranslateCommand;

public enum Digest {
    SHA256("Currently recommended, because the other algorithms" + " might break on some older versions of the GnuTLS library" + " (older than 3.x) still shipped in Debian for example."), SHA384(""), SHA512("Highest protection against hash collision attacks of the algorithms offered here.");

    private final Outputable exp;

    private Digest(String explanation) {
        exp = new TranslateCommand(explanation);
    }

    public Outputable getExp() {
        return exp;
    }

    public static Digest getDefault() {
        return SHA256;
    }

}
