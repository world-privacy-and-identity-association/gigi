package club.wpia.gigi.pages;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.util.AuthorizationContext;

public class LogoutPage extends Page {

    public static final String PATH = "/logout";

    public LogoutPage() {
        super("Logout");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession hs = req.getSession();
        if (req.getPathInfo() != null && req.getPathInfo().equals("/logout")) {
            if (hs != null) {
                hs.setAttribute(Gigi.LOGGEDIN, null);
                hs.invalidate();
            }
            resp.sendRedirect("/");
            return;
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null;
    }

}
