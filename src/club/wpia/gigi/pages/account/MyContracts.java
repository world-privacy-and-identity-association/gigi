package club.wpia.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Contract;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.Page;

public class MyContracts extends Page {

    public static final String PATH = "/account/contracts";

    public MyContracts() {
        super("My Contracts");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        Map<String, Object> vars = getDefaultVars(req);
        Language l = LoginPage.getLanguage(req);
        User u = getUser(req);
        vars.put("raname", u.getPreferredName());
        vars.put("csdate", l.getTranslation("not yet"));

        Contract c = Contract.getRAAgentContractByUser(u);
        if (c != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            vars.put("csdate", sdf.format(c.getDateSigned()));
        }

        getDefaultTemplate().output(out, getLanguage(req), vars);
    }
}
