package club.wpia.gigi;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeSet;

import club.wpia.gigi.database.DatabaseConnection;
import club.wpia.gigi.dbObjects.CertificateProfile;
import club.wpia.gigi.dbObjects.CertificateProfile.PropertyTemplate;

public class GenerateProfileOverview {

    public static void main(String[] args) throws IOException {
        Properties pr = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream("config/gigi.properties"), "UTF-8")) {
            pr.load(reader);
        }
        DatabaseConnection.init(pr);

        TreeSet<String> pt = new TreeSet<>();
        TreeSet<String> req = new TreeSet<>();
        LinkedList<CertificateProfile> cps = new LinkedList<>();
        for (CertificateProfile cp : CertificateProfile.getAll()) {
            cps.add(cp);
            for (PropertyTemplate p : cp.getTemplates().values()) {
                pt.add(p.getBase());
            }
            req.addAll(cp.getReqireds());
        }
        try (PrintWriter pw = new PrintWriter("profiles.html", "UTF-8")) {
            pw.println("<!DOCTYPE html><html><head><title>Profiles</title>");
            pw.println("<style>.split{background-color:#000;margin:0;cell-spacing:0}td{text-align:center}</style>");
            pw.println("</head>");
            pw.println("<body><table border='1'>");
            pw.println("<tr><td>id</td><td> </td>");
            for (String p : pt) {
                pw.println("<th>" + p + "</th>");
            }
            pw.println("<th class='split'></th>");
            for (String p : req) {
                pw.println("<th class='req'>" + p + "</th>");
            }
            pw.println("</tr>");
            for (CertificateProfile certificateProfile : cps) {
                pw.println("<tr>");
                pw.println("<td>" + certificateProfile.getId() + "</td>");
                pw.println("<td>" + certificateProfile.getKeyName() + "</td>");
                outer:
                for (String p : pt) {
                    for (PropertyTemplate t : certificateProfile.getTemplates().values()) {
                        if (t.getBase().equals(p)) {
                            pw.println("<td>" + (t.isRequired() ? (t.isMultiple() ? "+" : "y") : (t.isMultiple() ? "*" : "?")) + "</td>");
                            continue outer;
                        }
                    }
                    pw.println("<td></td>");
                }
                pw.println("<td class='split'></td>");
                outer:
                for (String p : req) {
                    for (String t : certificateProfile.getReqireds()) {
                        if (t.equals(p)) {
                            pw.println("<td class='req'>y</td>");
                            continue outer;
                        }
                    }
                    pw.println("<td></td>");
                }
                pw.println("</tr>");
            }
            pw.println("</table></body></html>");
            Desktop.getDesktop().browse(new File("profiles.html").toURI());
        }
    }

}
