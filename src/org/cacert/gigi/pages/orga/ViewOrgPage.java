package org.cacert.gigi.pages.orga;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class ViewOrgPage extends Page {

    private final Template orgas = new Template(ViewOrgPage.class.getResource("ViewOrgs.templ"));

    public static final String DEFAULT_PATH = "/orga";

    public ViewOrgPage() {
        super("View Organisation");
    }

    @Override
    public boolean isPermitted(User u) {
        return u != null && u.isInGroup(CreateOrgPage.ORG_ASSURER);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Form.getForm(req, CreateOrgForm.class).submit(resp.getWriter(), req);
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idS = req.getPathInfo();
        Language lang = getLanguage(req);
        PrintWriter out = resp.getWriter();
        if (idS.length() < DEFAULT_PATH.length() + 2) {
            final Organisation[] orgas = Organisation.getOrganisations(0, 30);
            HashMap<String, Object> map = new HashMap<>();
            map.put("orgas", new IterableDataset() {

                int count = 0;

                @Override
                public boolean next(Language l, Map<String, Object> vars) {
                    if (count >= orgas.length)
                        return false;
                    Organisation org = orgas[count++];
                    System.out.println(org.getId());
                    vars.put("id", Integer.toString(org.getId()));
                    vars.put("name", org.getName());
                    vars.put("country", org.getState());
                    return true;
                }
            });
            this.orgas.output(out, lang, map);
            return;
        }
        idS = idS.substring(DEFAULT_PATH.length() + 1);
        int id = Integer.parseInt(idS);
        Organisation o = Organisation.getById(id);
        if (o == null) {
            resp.sendError(404);
            return;
        }
        new CreateOrgForm(req, o).output(out, lang, new HashMap<String, Object>());
        out.println(lang.getTranslation("WARNING: updating the data will revoke all issued certificates."));
    }
}
