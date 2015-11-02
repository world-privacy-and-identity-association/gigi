package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.util.AuthorizationContext;

public class MyOrganisationsForm extends Form {

    private AuthorizationContext target;

    public MyOrganisationsForm(HttpServletRequest hsr) {
        super(hsr);
        target = LoginPage.getAuthorizationContext(hsr);
    }

    private static Template template;

    static {
        template = new Template(MyListingForm.class.getResource("MyOrganisationsForm.templ"));
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        if (req.getParameter("org-leave") != null) {
            req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(target.getActor(), target.getActor()));
            return true;
        }
        Enumeration<String> i = req.getParameterNames();
        int orgId = -1;
        while (i.hasMoreElements()) {
            String s = i.nextElement();
            if (s.startsWith("org:")) {
                int id = Integer.parseInt(s.substring(4));
                if (orgId == -1) {
                    orgId = id;
                } else {
                    out.println(LoginPage.getLanguage(req).getTranslation("Error: invalid parameter."));
                    return false;
                }
            }
        }
        for (Organisation org : target.getActor().getOrganisations()) {
            if (org.getId() == orgId) {

                req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(org, target.getActor()));
                return true;
            }
        }
        System.out.println("Switch fialed");
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final List<Organisation> o = target.getActor().getOrganisations();
        if (target.getTarget() != target.getActor()) {
            vars.put("personal", target.getTarget() != target.getActor());
        }
        vars.put("orgas", new IterableDataset() {

            Iterator<Organisation> it = o.iterator();

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if ( !it.hasNext()) {
                    return false;
                }
                Organisation o = it.next();
                vars.put("orgName", o.getName());
                vars.put("orgID", o.getId());
                return true;
            }
        });
        template.output(out, l, vars);

    }

}
