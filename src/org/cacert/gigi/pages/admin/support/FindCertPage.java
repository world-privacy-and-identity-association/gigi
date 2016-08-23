package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.SubjectAlternateName;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.ArrayIterable;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.account.certs.Certificates;
import org.cacert.gigi.util.AuthorizationContext;

public class FindCertPage extends Page {

    public static final String PATH = "/support/find/certs";

    public FindCertPage() {
        super("Find Certificate");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new FindCertForm(req).output(resp.getWriter(), Page.getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FindCertForm form = Form.getForm(req, FindCertForm.class);
        if (form.submitProtected(resp.getWriter(), req)) {
            final Certificate[] certs = form.getCerts();
            if (certs.length == 1) {
                resp.sendRedirect(Certificates.SUPPORT_PATH + "/" + certs[0].getSerial());
            } else {
                HashMap<String, Object> vars = new HashMap<String, Object>();
                Language l = LoginPage.getLanguage(req);
                if (certs.length >= 100) {
                    vars.put("limit", l.getTranslation("100 or more entries available, only the first 100 are displayed."));
                } else {
                    vars.put("limit", SprintfCommand.createSimple("{0} entries found", certs.length));
                }
                vars.put("certtable", new ArrayIterable<Certificate>(certs) {

                    @Override
                    public void apply(Certificate t, Language l, Map<String, Object> vars) {
                        vars.put("id", t.getId());
                        vars.put("serial", t.getSerial());

                        final List<SubjectAlternateName> san = t.getSANs();
                        vars.put("san", new IterableDataset() {

                            int j = 0;

                            @Override
                            public boolean next(Language l, Map<String, Object> vars) {
                                if (j == san.size()) {
                                    return false;
                                }
                                vars.put("entry", san.get(j).getName() + (j < san.size() - 1 ? ", " : ""));
                                j++;
                                return true;
                            }

                        });
                    }
                });
                getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
            }
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }
}
