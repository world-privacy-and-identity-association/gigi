package org.cacert.gigi.dbObjects;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.database.GigiResultSet;

public class CertificateProfile implements IdCachable {

    private final int id;

    private final String keyName;

    private final String visibleName;

    private static HashMap<String, CertificateProfile> byName = new HashMap<>();

    private static HashMap<Integer, CertificateProfile> byId = new HashMap<>();

    private final Map<String, PropertyTemplate> pt;

    private final List<String> req;

    private CertificateProfile(int id, String keyName, String visibleName, String requires, String include) {
        this.id = id;
        this.keyName = keyName;
        this.visibleName = visibleName;
        req = parseConditions(requires);
        pt = parsePropertyTemplates(include);
    }

    public static class PropertyTemplate implements Comparable<PropertyTemplate> {

        private boolean required = true;

        private boolean multiple = false;

        private String base;

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
            this.base = inc;
        }

        public String getBase() {
            return base;
        }

        public boolean isMultiple() {
            return multiple;
        }

        public boolean isRequired() {
            return required;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((base == null) ? 0 : base.hashCode());
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
            if (base == null) {
                if (other.base != null) {
                    return false;
                }
            } else if ( !base.equals(other.base)) {
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
            return base + (multiple ? (required ? "+" : "*") : (required ? "" : "?"));
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

    private List<String> parseConditions(String property) {
        String[] split2 = property.split(",");
        if (split2.length == 1 && split2[0].equals("")) {
            split2 = new String[0];
        }
        return Collections.unmodifiableList(Arrays.asList(split2));
    }

    private Map<String, PropertyTemplate> parsePropertyTemplates(String property) {
        String[] split = property.split(",");
        HashMap<String, PropertyTemplate> map = new HashMap<>(split.length);
        for (int i = 0; i < split.length; i++) {

            PropertyTemplate value = new PropertyTemplate(split[i]);

            map.put(value.getBase(), value);
        }
        return Collections.unmodifiableMap(map);
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

    public Map<String, PropertyTemplate> getTemplates() {
        return pt;
    }

    public List<String> getReqireds() {
        return req;
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
