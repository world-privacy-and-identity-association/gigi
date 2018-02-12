package club.wpia.gigi.pages.admin.support;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.Organisation;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

public class SupportOrgDomainPage extends Page {

    public static final String PATH = "/support/domain/";

    public SupportOrgDomainPage() {
        super("Support: Organisation Domain");
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Domain orgDomain = getDomain(req, resp);
        if (orgDomain == null) {
            return;
        }

        Organisation org = Organisation.getById(orgDomain.getOwner().getId());
        Map<String, Object> vars = getDefaultVars(req);
        vars.put("domain", orgDomain.getSuffix());
        vars.put("organisation", org.getName());

        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    private Domain getDomain(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = -1;
        String[] idP = req.getPathInfo().split("/");
        try {
            id = Integer.parseInt(idP[idP.length - 1]);
        } catch (NumberFormatException e) {
            resp.sendError(400);
            return null;
        }
        final Domain domain = Domain.getById(id);
        if (domain == null) {
            resp.sendError(400);
            return null;
        }
        return domain;
    }

}
