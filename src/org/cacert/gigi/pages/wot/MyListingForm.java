package org.cacert.gigi.pages.wot;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class MyListingForm extends Form {

    private static Template template;

    static {
        template = new Template(MyListingForm.class.getResource("MyListingForm.templ"));
    }

    private User target;

    public MyListingForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        if (req.getParameter("listme") != null && req.getParameter("contactinfo") != null) {
            boolean on = !req.getParameter("listme").equals("0");
            target.setDirectoryListing(on);
            if (on) {
                target.setContactInformation(req.getParameter("contactinfo"));
            } else {
                target.setContactInformation("");
            }
            return true;
        }
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        if (target.wantsDirectoryListing()) {
            vars.put("selected", "selected");
            vars.put("notSelected", "");
            vars.put("activeInfo", target.getContactInformation());
        } else {
            vars.put("selected", "");
            vars.put("notSelected", "selected");
            vars.put("activeInfo", target.getContactInformation());
        }
        template.output(out, l, vars);
    }

}