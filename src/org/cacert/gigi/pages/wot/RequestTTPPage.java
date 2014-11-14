package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Assurance;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class RequestTTPPage extends Page {

    public static final String PATH = "/wot/ttp";

    public RequestTTPPage() {
        super("Request TTP");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Form.getForm(req, RequestTTPForm.class).submit(resp.getWriter(), req);
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User u = LoginPage.getUser(req);
        HashMap<String, Object> map = new HashMap<String, Object>();
        if (u.isInGroup(RequestTTPForm.TTP_APPLICANT)) {
            map.put("inProgress", true);
        } else {
            if (u.getAssurancePoints() < 100) {
                int ttpCount = 0;
                for (Assurance a : u.getReceivedAssurances()) {
                    if (a.getMethod().equals(Assurance.AssuranceType.TTP_ASSISTED.getDescription())) {
                        ttpCount++;
                    }
                }
                if (ttpCount < 2) {
                    map.put("ttp", true);
                    map.put("form", new RequestTTPForm(req));
                } else {
                    map.put("nothing", true);
                }
            } else {
                map.put("nothing", true);
            }
        }
        map.put("form", new RequestTTPForm(req));
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), map);
    }

}
