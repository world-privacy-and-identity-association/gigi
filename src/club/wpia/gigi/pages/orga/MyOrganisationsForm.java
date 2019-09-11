package club.wpia.gigi.pages.orga;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.util.AuthorizationContext;

public class MyOrganisationsForm extends Form {

    private AuthorizationContext target;

    public MyOrganisationsForm(HttpServletRequest hsr) {
        super(hsr);
        target = LoginPage.getAuthorizationContext(hsr);
    }

    private static final Template template = new Template(MyOrganisationsForm.class.getResource("MyOrganisationsForm.templ"));

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        AuthorizationContext sessionAc = (AuthorizationContext) req.getSession().getAttribute(Gigi.AUTH_CONTEXT);
        if (req.getParameter("org-leave") != null) {
            req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(target.getActor(), target.getActor(), sessionAc.isStronglyAuthenticated()));
            return new RedirectResult(SwitchOrganisation.PATH);
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
                    throw new GigiApiException("Error: invalid parameter.");
                }
            }
        }
        for (Organisation org : target.getActor().getOrganisations()) {
            if (org.getId() == orgId) {

                req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(org, target.getActor(), sessionAc.isStronglyAuthenticated()));
                return new RedirectResult(SwitchOrganisation.PATH);
            }
        }
        throw new PermamentFormException(new GigiApiException("Context switch failed."));
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final List<Organisation> o = target.getActor().getOrganisations();
        vars.put("certlogin", target.isStronglyAuthenticated());
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
