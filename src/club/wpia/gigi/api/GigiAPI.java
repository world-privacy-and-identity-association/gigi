package club.wpia.gigi.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.database.DatabaseConnection.Link;

public class GigiAPI extends HttpServlet {

    private static final long serialVersionUID = 659963677032635817L;

    HashMap<String, APIPoint> api = new HashMap<>();

    public GigiAPI() {
        api.put(CreateCertificate.PATH, new CreateCertificate());
        api.put(Emails.PATH, new Emails());
        api.put(EmailReping.PATH, new EmailReping());
        api.put(RevokeCertificate.PATH, new RevokeCertificate());
        api.put(CATSImport.PATH, new CATSImport());
        api.put(CATSResolve.PATH, new CATSResolve());
        FindAgent.register(api);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pi = req.getPathInfo();
        if (pi == null) {
            return;
        }
        if (pi.equals("/security/csp/report")) {
            ServletInputStream sis = req.getInputStream();
            InputStreamReader isr = new InputStreamReader(sis, "UTF-8");
            StringBuffer strB = new StringBuffer();
            char[] buffer = new char[4 * 1024];
            int len;
            while ((len = isr.read(buffer)) > 0) {
                strB.append(buffer, 0, len);
            }
            System.out.println(strB);
            return;
        }

        APIPoint p = api.get(pi);
        try (Link l = DatabaseConnection.newLink(false)) {
            if (p != null) {
                p.process(req, resp);
            }
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }
}
