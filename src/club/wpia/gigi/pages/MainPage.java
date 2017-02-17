package club.wpia.gigi.pages;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.output.template.Template;

public class MainPage extends Page {

    private static final Template notLog = new Template(MainPage.class.getResource("MainPageNotLogin.templ"));

    public MainPage() {
        super("Home");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> vars = getDefaultVars(req);
        if (LoginPage.getUser(req) != null) {
            getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
        } else {
            notLog.output(resp.getWriter(), getLanguage(req), vars);
        }
    }

    @Override
    public boolean needsLogin() {
        return false;
    }
}
