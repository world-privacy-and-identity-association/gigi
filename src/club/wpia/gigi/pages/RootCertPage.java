package club.wpia.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CACertificate;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.util.CertExporter;
import club.wpia.gigi.util.HTMLEncoder;
import club.wpia.gigi.util.PEM;
import club.wpia.gigi.util.ServerConstants;

public class RootCertPage extends Page {

    private final Certificate root;

    private final CACertificate[] cs;

    private final OutputableCertificate rootP;

    private final String appName = ServerConstants.getAppName().toLowerCase();

    private class OutputableCertificate implements Outputable {

        private final CACertificate target;

        private final OutputableCertificate[] children;

        public OutputableCertificate(CACertificate c) {
            target = c;
            LinkedList<OutputableCertificate> children = new LinkedList<>();
            for (CACertificate c0 : cs) {
                if (c0.getParent() == c && c0 != c) {
                    children.add(new OutputableCertificate(c0));
                }
            }

            Collections.sort(children, new Comparator<OutputableCertificate>() {

                @Override
                public int compare(OutputableCertificate o1, OutputableCertificate o2) {
                    return o1.target.getKeyname().compareTo(o2.target.getKeyname());
                }
            });
            this.children = children.toArray(new OutputableCertificate[children.size()]);
        }

        @Override
        public void output(PrintWriter out, Language l, Map<String, Object> vars) {
            out.println("<a href='" + HTMLEncoder.encodeHTML(target.getLink()) + "' download='" + HTMLEncoder.encodeHTML(target.getKeyname()) + "'>");
            out.println(HTMLEncoder.encodeHTML(target.getKeyname()));
            out.println("</a>");
            out.println(HTMLEncoder.encodeHTML(target.getCertificate().getSubjectX500Principal().toString()));
            out.println("<ul>");
            for (OutputableCertificate c : children) {
                out.print("<li>");
                c.output(out, l, vars);
                out.print("</li>");
            }
            out.println("</ul>");
        }

    }

    public RootCertPage(KeyStore ks) {
        super("Root Certificates");
        try {
            root = ks.getCertificate("root");
        } catch (KeyStoreException e) {
            throw new Error(e);
        }
        cs = CACertificate.getAll();
        CACertificate rootC = null;
        for (CACertificate c : cs) {
            if (c.isSelfsigned()) {
                rootC = c;
                break;
            }
        }
        if (rootC == null) {
            throw new Error();
        }
        rootP = new OutputableCertificate(rootC);
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("pem") != null && root != null) {
            resp.setContentType("application/x-x509-ca-cert");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + appName + "_roots.crt\"");
            ServletOutputStream out = resp.getOutputStream();
            try {
                out.println(PEM.encode("CERTIFICATE", root.getEncoded()));
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
            return true;
        } else if (req.getParameter("bundle") != null && root != null) {
            resp.setContentType("application/x-x509-ca-cert");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + appName + "_intermediate_bundle.p7b\"");
            ServletOutputStream out = resp.getOutputStream();
            try {
                CertExporter.writeCertBundle(out);
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (GigiApiException e) {
                e.printStackTrace();
            }
            return true;
        } else if (req.getParameter("cer") != null && root != null) {
            resp.setContentType("application/x-x509-ca-cert");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + appName + "_roots.cer\"");
            ServletOutputStream out = resp.getOutputStream();
            try {
                out.write(root.getEncoded());
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> map = Page.getDefaultVars(req);
        map.put("root", rootP);
        map.put("bundle", appName + "_intermediate_bundle.p7b");

        try {
            map.put("fingerprintSHA1", rootP.target.getFingerprint("sha-1"));
            map.put("fingerprintSHA256", rootP.target.getFingerprint("sha-256"));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), map);
    }

    @Override
    public boolean needsLogin() {
        return false;
    }
}
