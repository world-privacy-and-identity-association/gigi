package org.cacert.gigi.dbObjects;

import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.TranslateCommand;
import org.cacert.gigi.output.template.SprintfCommand;
import java.util.Arrays;

public enum Digest {
    SHA256(new SprintfCommand("Most compatible choice (see {0}documentation{1} for details)", Arrays.asList("!'<a href='//links.teracara.org/sha2-256'>", "!'</a>"))),
    SHA384("Best matched with ECC P-384"),
    SHA512("Highest collision resistance, recommended");

    private final Outputable exp;

    private Digest(String explanation) {
        exp = new TranslateCommand(explanation);
    }

    private Digest(Outputable exp) {
        this.exp = exp;
    }

    public Outputable getExp() {
        return exp;
    }

    public static Digest getDefault() {
        return SHA512;
    }

}
