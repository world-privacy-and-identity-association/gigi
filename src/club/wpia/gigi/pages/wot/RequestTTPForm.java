package club.wpia.gigi.pages.wot;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.OutputableArrayIterable;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.LoginPage;

public class RequestTTPForm extends Form {

    public static final Group TTP_APPLICANT = Group.TTP_APPLICANT;

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
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String country = req.getParameter("country");
        if (country != null) {
            int cid = Integer.parseInt(country);
            if (cid < 0 || cid >= COUNTRIES.length) {
                throw new GigiApiException("Invalid country id");
            }
            country = COUNTRIES[cid];
        }
        // TODO use country?

        User uReq = LoginPage.getUser(req);

        if ( !u.equals(uReq)) {
            throw new GigiApiException("Internal logic error.");
        }

        u.grantGroup(u, TTP_APPLICANT);
        return new SuccessMessageResult(new TranslateCommand("Successfully applied for TTP."));
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> map) {
        map.put("countries", new OutputableArrayIterable(COUNTRIES, "country"));

        t.output(out, l, map);
    }

}
