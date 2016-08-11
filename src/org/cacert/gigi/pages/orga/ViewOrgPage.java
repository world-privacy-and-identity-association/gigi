package org.cacert.gigi.pages.orga;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.account.domain.DomainManagementForm;
import org.cacert.gigi.util.AuthorizationContext;

public class ViewOrgPage extends Page {

    private static final Template orgas = new Template(ViewOrgPage.class.getResource("ViewOrgs.templ"));

    private static final Template mainTempl = new Template(ViewOrgPage.class.getResource("EditOrg.templ"));

    public static final String DEFAULT_PATH = "/orga";

    public ViewOrgPage() {
        super("View Organisation");
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && (ac.isInGroup(CreateOrgPage.ORG_ASSURER) || ac.getActor().getOrganisations(true).size() != 0);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = LoginPage.getUser(req);
        if (req.getParameter("do_affiliate") != null || req.getParameter("del") != null) {
            AffiliationForm form = Form.getForm(req, AffiliationForm.class);
            if (form.submitProtected(resp.getWriter(), req)) {
                resp.sendRedirect(DEFAULT_PATH + "/" + form.getOrganisation().getId());
            }
            return;
        } else {
            if ( !u.isInGroup(CreateOrgPage.ORG_ASSURER)) {
                resp.sendError(403, "Access denied");
                return;
            }

            if (req.getParameter("addDomain") != null) {
                OrgDomainAddForm form = Form.getForm(req, OrgDomainAddForm.class);
                if (form.submitProtected(resp.getWriter(), req)) {
                    resp.sendRedirect(DEFAULT_PATH + "/" + form.getOrganisation().getId());
                }
            } else if (req.getParameter("delete") != null) {
                DomainManagementForm form = Form.getForm(req, DomainManagementForm.class);
                if (form.submitProtected(resp.getWriter(), req)) {
                    resp.sendRedirect(DEFAULT_PATH + "/" + form.getTarget().getId());
                }
            } else {
                CreateOrgForm form = Form.getForm(req, CreateOrgForm.class);
                if (form.submitProtected(resp.getWriter(), req)) {
                    resp.sendRedirect(DEFAULT_PATH + "/" + form.getResult().getId());
                }
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
            final boolean orgAss = u.isInGroup(CreateOrgPage.ORG_ASSURER);
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
        final boolean orgAss = u.isInGroup(CreateOrgPage.ORG_ASSURER);
        if ( !orgAss && !myOrgs.contains(o)) {
            resp.sendError(404);
            return;
        }
        HashMap<String, Object> vars = new HashMap<>();
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
                vars.put("country", org.getState());
                return true;
            }
        };
    }
}
