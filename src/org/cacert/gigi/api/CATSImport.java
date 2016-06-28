package org.cacert.gigi.api;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.CATS;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Organisation;
import org.cacert.gigi.dbObjects.User;

public class CATSImport extends APIPoint {

    public static final String PATH = "/cats/import";

    @Override
    public void process(HttpServletRequest req, HttpServletResponse resp, CertificateOwner u) throws IOException {
        if ( !(u instanceof Organisation)) {
            resp.sendError(500, "Error, invalid cert");
            return;
        }
        if ( !((Organisation) u).isSelfOrganisation()) {
            resp.sendError(500, "Error, invalid cert");
            return;

        }
        String target = req.getParameter("mid");
        String testType = req.getParameter("variant");
        String date = req.getParameter("date");
        if (target == null || testType == null || date == null) {
            resp.sendError(500, "Error, requires mid, variant and date");
            return;
        }
        String language = req.getParameter("language");
        String version = req.getParameter("version");
        if (language == null || version == null) {
            resp.sendError(500, "Error, requires also language and version");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(target);
        } catch (NumberFormatException e) {
            resp.sendError(500, "Error, requires mid to be integer.");
            return;
        }
        CertificateOwner o = CertificateOwner.getById(id);
        if ( !(o instanceof User)) {
            resp.sendError(500, "Error, requires valid userid");
            return;
        }
        System.out.println("CATS: " + target + ": " + testType);
        User targetUser = (User) o;
        System.out.println(targetUser.getId());
        CATS.enterResult(targetUser, testType, new Date(Long.parseLong(date)), language, version);
    }
}
