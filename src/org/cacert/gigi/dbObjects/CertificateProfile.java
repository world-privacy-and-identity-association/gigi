package org.cacert.gigi.dbObjects;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeSet;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public class CertificateProfile {

    private final int id;

    private final String keyName;

    private final String visibleName;

    private static HashMap<String, CertificateProfile> byName = new HashMap<>();

    private static HashMap<Integer, CertificateProfile> byId = new HashMap<>();

    private final PropertyTemplate[] pt;

    private final String[] req;

    private CertificateProfile(int id, String keyName, String visibleName, String requires, String include) {
        this.id = id;
        this.keyName = keyName;
        this.visibleName = visibleName;
        req = parseConditions(requires);
        pt = parsePropertyTemplates(include);
    }

    private static class PropertyTemplate implements Comparable<PropertyTemplate> {

        boolean required = true;

        boolean multiple = false;

        private String inc;

        public PropertyTemplate(String inc) {
            if (inc.endsWith("?") || inc.endsWith("*") || inc.endsWith("+")) {
                char sfx = inc.charAt(inc.length() - 1);
                if (sfx == '?') {
                    required = false;
                } else if (sfx == '*') {
                    multiple = true;
                    required = false;
                } else {
                    multiple = true;
                }
                inc = inc.substring(0, inc.length() - 1);
            }
            this.inc = inc;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((inc == null) ? 0 : inc.hashCode());
            result = prime * result + (multiple ? 1231 : 1237);
            result = prime * result + (required ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PropertyTemplate other = (PropertyTemplate) obj;
            if (inc == null) {
                if (other.inc != null) {
                    return false;
                }
            } else if ( !inc.equals(other.inc)) {
                return false;
            }
            if (multiple != other.multiple) {
                return false;
            }
            if (required != other.required) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return inc + (multiple ? (required ? "+" : "*") : (required ? "" : "?"));
        }

        @Override
        public int compareTo(PropertyTemplate o) {
            return toString().compareTo(o.toString());
        }

    }

    private CertificateProfile(File f) throws IOException {
        Properties p = new Properties();
        p.load(new FileInputStream(f));
        String[] parts = f.getName().split("\\.")[0].split("-", 2);
        id = Integer.parseInt(parts[0]);
        keyName = parts[1];
        visibleName = "";
        pt = parsePropertyTemplates(p.getProperty("include"));
        req = parseConditions(p.getProperty("requires", ""));
    }

    private String[] parseConditions(String property) {
        String[] split2 = property.split(",");
        if (split2.length == 1 && split2[0].equals("")) {
            split2 = new String[0];
        }
        return split2;
    }

    private PropertyTemplate[] parsePropertyTemplates(String property) {
        String[] split = property.split(",");
        PropertyTemplate[] pt = new PropertyTemplate[split.length];
        for (int i = 0; i < split.length; i++) {
            pt[i] = new PropertyTemplate(split[i]);
        }
        return pt;
    }

    public int getId() {
        return id;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getVisibleName() {
        return visibleName;
    }

    static {
        for (File f : new File("config/profiles").listFiles()) {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(f));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] parts = f.getName().split("\\.")[0].split("-", 2);
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT keyname, include, requires, name FROM `profiles` WHERE id=?");
            ps.setInt(1, Integer.parseInt(parts[0]));
            GigiResultSet rs = ps.executeQuery();

            if (rs.next()) {
                if ( !rs.getString("keyname").equals(parts[1])) {
                    throw new Error("Config error. Certificate Profile mismatch");
                }
                if ( !rs.getString("include").equals(p.getProperty("include"))) {
                    throw new Error("Config error. Certificate Profile mismatch");
                }
                if ( !rs.getString("requires").equals(p.getProperty("requires", ""))) {
                    throw new Error("Config error. Certificate Profile mismatch");
                }
            } else {
                GigiPreparedStatement insert = DatabaseConnection.getInstance().prepare("INSERT INTO `profiles` SET keyname=?, include=?, requires=?, name=?, id=?");
                insert.setString(1, parts[1]);
                insert.setString(2, p.getProperty("include"));
                insert.setString(3, p.getProperty("requires", ""));
                insert.setString(4, p.getProperty("name"));
                insert.setInt(5, Integer.parseInt(parts[0]));
                insert.execute();
            }

        }
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT id, keyname, name, requires, include FROM `profiles`");
        GigiResultSet rs = ps.executeQuery();
        while (rs.next()) {
            CertificateProfile cp = new CertificateProfile(rs.getInt("id"), rs.getString("keyName"), rs.getString("name"), rs.getString("requires"), rs.getString("include"));
            byId.put(cp.getId(), cp);
            byName.put(cp.getKeyName(), cp);
        }

    }

    public static CertificateProfile getById(int id) {
        return byId.get(id);
    }

    public static CertificateProfile getByName(String name) {
        return byName.get(name);
    }

    public static CertificateProfile[] getAll() {
        return byId.values().toArray(new CertificateProfile[byId.size()]);
    }

    public static void main(String[] args) throws IOException {
        TreeSet<String> pt = new TreeSet<>();
        TreeSet<String> req = new TreeSet<>();
        LinkedList<CertificateProfile> cps = new LinkedList<>();
        for (CertificateProfile cp : byId.values()) {
            cps.add(cp);
            for (PropertyTemplate p : cp.pt) {
                pt.add(p.inc);
            }
            req.addAll(Arrays.asList(cp.req));
        }
        PrintWriter pw = new PrintWriter("profiles.html");
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
            pw.println("<td>" + certificateProfile.id + "</td>");
            pw.println("<td>" + certificateProfile.keyName + "</td>");
            outer:
            for (String p : pt) {
                for (PropertyTemplate t : certificateProfile.pt) {
                    if (t.inc.equals(p)) {
                        pw.println("<td>" + (t.required ? (t.multiple ? "+" : "y") : (t.multiple ? "*" : "?")) + "</td>");
                        continue outer;
                    }
                }
                pw.println("<td></td>");
            }
            pw.println("<td class='split'></td>");
            outer:
            for (String p : req) {
                for (String t : certificateProfile.req) {
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
        pw.close();
    }

    public boolean canBeIssuedBy(User u) {
        for (String s : req) {
            if (s.equals("points>=50")) {
                if (u.getAssurancePoints() < 50) {
                    return false;
                }
            } else if (s.equals("points>=100")) {
                if (u.getAssurancePoints() < 100) {
                    return false;
                }
            } else if (s.equals("codesign")) {
                if (u.isInGroup(Group.CODESIGNING)) {
                    return false;
                }
            } else {
                return false;
            }

        }
        return true;
    }
}
