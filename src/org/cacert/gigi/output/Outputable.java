package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;

public interface Outputable {

    public void output(PrintWriter out, Language l, Map<String, Object> vars);
}
