package club.wpia.gigi.pages.orga;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Form.CSRFException;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.ManagedMultiFormPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.pages.account.domain.DomainManagementForm;
import club.wpia.gigi.util.AuthorizationContext;

public class ViewOrgPage extends ManagedMultiFormPage {

    private static final Template orgas = new Template(ViewOrgPage.class.getResource("ViewOrgs.templ"));

    private static final Template mainTempl = new Template(ViewOrgPage.class.getResource("EditOrg.templ"));

    public static final String DEFAULT_PATH = "/orga";

    public ViewOrgPage() {
        super("View Organisation");
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && (ac.isInGroup(CreateOrgPage.ORG_AGENT) || ac.getActor().getOrganisations(true).size() != 0);
    }

    @Override
    public Form getForm(HttpServletRequest req) throws CSRFException {
        if (req.getParameter("do_affiliate") != null || req.getParameter("del") != null) {
            return Form.getForm(req, AffiliationForm.class);
        } else {
            if ( !getUser(req).isInGroup(CreateOrgPage.ORG_AGENT)) {
                return null;
            }

            if (req.getParameter("addDomain") != null) {
                return Form.getForm(req, OrgDomainAddForm.class);
            } else if (req.getParameter("delete") != null) {
                return Form.getForm(req, DomainManagementForm.class);
            } else {
                return Form.getForm(req, CreateOrgForm.class);
            }
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = LoginPage.getUser(req);
        String idS = req.getPathInfo();
        Language lang = getLanguage(req);
        PrintWriter out = resp.getWriter();
        if (idS.length() < DEFAULT_PATH.length() + 2) {
            final Organisation[] orgList = Organisation.getOrganisations(0, 30);
            HashMap<String, Object> map = new HashMap<>();
            final List<Organisation> myOrgs = u.getOrganisations(true);
            final boolean orgAss = u.isInGroup(CreateOrgPage.ORG_AGENT);
            if (orgAss) {
                map.put("orgas", makeOrgDataset(orgList));
            } else {
                map.put("orgas", makeOrgDataset(myOrgs.toArray(new Organisation[myOrgs.size()])));
            }
            orgas.output(out, lang, map);
            return;
        }
        idS = idS.substring(DEFAULT_PATH.length() + 1);
        int id = Integer.parseInt(idS);
        Organisation o;
        try {
            o = Organisation.getById(id);
        } catch (IllegalArgumentException e) {
            resp.sendError(404);
            return;
        }
        final List<Organisation> myOrgs = u.getOrganisations();
        final boolean orgAss = u.isInGroup(CreateOrgPage.ORG_AGENT);
        if ( !orgAss && !myOrgs.contains(o)) {
            resp.sendError(404);
            return;
        }
        Map<String, Object> vars = Page.getDefaultVars(req);
        if (orgAss) {
            vars.put("editForm", new CreateOrgForm(req, o));
            vars.put("affForm", new AffiliationForm(req, o));
            vars.put("mgmDom", new DomainManagementForm(req, o, true));
            vars.put("addDom", new OrgDomainAddForm(req, o));
        } else {
            vars.put("affForm", new AffiliationForm(req, o));
            vars.put("orgName", o.getName());
        }
        mainTempl.output(out, lang, vars);
    }

    private IterableDataset makeOrgDataset(final Organisation[] orgas) {
        return new IterableDataset() {

            int count = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (count >= orgas.length) {
                    return false;
                }
                Organisation org = orgas[count++];
                vars.put("id", Integer.toString(org.getId()));
                vars.put("name", org.getName());
                vars.put("country", org.getCountry().getCode());
                return true;
            }
        };
    }
}
