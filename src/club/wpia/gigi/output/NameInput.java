package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.NamePart;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.output.template.Template;

public class NameInput implements Outputable {

    private static final Template t = new Template(NameInput.class.getResource("NameInput.templ"));

    private String fname = "";

    private String lname = "";

    private String suffix = "";

    private String name = "";

    private String scheme = "western";

    public NameInput() {}

    public void update(HttpServletRequest req) throws GigiApiException {
        fname = req.getParameter("fname");
        lname = req.getParameter("lname");
        suffix = req.getParameter("suffix");
        name = req.getParameter("name");
        scheme = req.getParameter("name-type");
        if (fname == null) {
            fname = "";
        }
        if (lname == null) {
            lname = "";
        }
        if (suffix == null) {
            suffix = "";
        }
        if (name == null) {
            name = "";
        }
        if ( !"western".equals(scheme) && !"single".equals(scheme)) {
            throw new GigiApiException("Invalid name type.");
        }
        if (name.contains(" ")) {
            throw new GigiApiException("Single names may only have one part.");
        }

    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("fname", fname);
        vars.put("lname", lname);
        vars.put("suffix", suffix);
        vars.put("name", name);
        vars.put("western", "western".equals(scheme));
        vars.put("single", "single".equals(scheme));
        t.output(out, l, vars);
    }

    public void createName(User u) throws GigiApiException {
        new Name(u, getNameParts());
    }

    public NamePart[] getNameParts() throws GigiApiException {
        if ("single".equals(scheme)) {
            return new NamePart[] {
                    new NamePart(NamePartType.SINGLE_NAME, name)
            };
        }
        String[] fparts = split(fname);
        String[] lparts = split(lname);
        String[] suff = split(suffix);
        if (fparts.length == 0 || fparts[0].equals("") || lparts.length == 0 || lparts[0].equals("")) {
            throw new GigiApiException("requires at least one first and one last name");
        }
        NamePart[] np = new NamePart[fparts.length + lparts.length + suff.length];
        int p = 0;
        for (int i = 0; i < fparts.length; i++) {
            np[p++] = new NamePart(NamePartType.FIRST_NAME, fparts[i]);
        }
        for (int i = 0; i < lparts.length; i++) {
            np[p++] = new NamePart(NamePartType.LAST_NAME, lparts[i]);
        }
        for (int i = 0; i < suff.length; i++) {
            np[p++] = new NamePart(NamePartType.SUFFIX, suff[i]);
        }

        return np;
    }

    private String[] split(String toSplit) {
        if (toSplit == null || toSplit.trim().isEmpty()) {
            return new String[0];
        }
        return toSplit.split(" ");
    }

    public String[] getNamePartsPlain() throws GigiApiException {
        NamePart[] p = getNameParts();
        String[] s = new String[p.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = p[i].getValue();
        }
        return s;
    }
}
