package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.util.HTMLEncoder;

public class GroupSelector implements Outputable {

    private final String name;

    private Group value = null;

    private final boolean supportFlag;

    public GroupSelector(String name, boolean supportFlag) {
        this.name = HTMLEncoder.encodeHTML(name);
        this.supportFlag = supportFlag;
    }

    public void update(HttpServletRequest r) throws GigiApiException {
        String vS = r.getParameter(name);
        value = null;
        for (Group g : Group.values()) {
            if (g.getDatabaseName().equals(vS)) {
                value = g;
            }
        }
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<select name='" + name + "'>");
        for (Group g : Group.values()) {
            if (supportFlag == g.isManagedBySupport()) {
                out.print("<option name='" + g.getDatabaseName());
                if (g.equals(value)) {
                    out.print(" selected");
                }
                out.println("'>" + g.getDatabaseName() + "</option>");
            }
        }
        out.println("</select>");
    }

    public Group getGroup() {
        return value;
    }
}
