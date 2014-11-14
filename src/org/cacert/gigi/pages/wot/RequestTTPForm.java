package org.cacert.gigi.pages.wot;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.OutputableArrayIterable;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

public class RequestTTPForm extends Form {

    public static final Group TTP_APPLICANT = Group.getByString("ttp-applicant");

    private static final Template t = new Template(RequestTTPForm.class.getResource("RequestTTPForm.templ"));

    private User u;

    public RequestTTPForm(HttpServletRequest hsr) {
        super(hsr);
        u = LoginPage.getUser(hsr);
    }

    private final String[] COUNTRIES = new String[] {
            "Australia", "Puerto Rico", "USA"
    };

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        String country = req.getParameter("country");
        if (country != null) {
            int cid = Integer.parseInt(country);
            if (cid < 0 || cid >= COUNTRIES.length) {
                throw new GigiApiException("Invalid country id");
            }
            country = COUNTRIES[cid];
        }
        User u = LoginPage.getUser(req);
        u.grantGroup(u, TTP_APPLICANT);

        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> map) {
        map.put("countries", new OutputableArrayIterable(COUNTRIES, "country"));

        t.output(out, l, map);
    }

}
