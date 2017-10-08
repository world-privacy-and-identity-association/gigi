package club.wpia.gigi.dbObjects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;

import javax.security.auth.x500.X500Principal;

import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class CACertificate implements IdCachable {

    private final String keyname;

    private final int id;

    private CACertificate parent = null;

    private final X509Certificate cert;

    private final String link;

    private static final CACertificate[] instances;

    private static ObjectCache<CACertificate> myCache = new ObjectCache<>();

    private CACertificate(int id) {
        this.id = id;
        int parentRoot;
        try (GigiPreparedStatement conn = new GigiPreparedStatement("SELECT `keyname`, `parentRoot`, `link` FROM `cacerts` WHERE `id`=?")) {
            conn.setInt(1, id);
            GigiResultSet res = conn.executeQuery();
            if ( !res.next()) {
                throw new IllegalArgumentException();
            }
            keyname = res.getString("keyname");
            link = res.getString("link");
            parentRoot = res.getInt("parentRoot");
            if (res.next()) {
                throw new RuntimeException("DB is broken");
            }
        }
        if (parentRoot == id) {
            parent = this;
        } else {
            parent = getById(parentRoot);
        }
        try {
            FileInputStream fis = new FileInputStream("config/ca/" + keyname + ".crt");
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            cert = (X509Certificate) cf.generateCertificate(fis);
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (GeneralSecurityException e) {
            throw new Error(e);
        }
    }

    public CACertificate getParent() {
        return parent;
    }

    public X509Certificate getCertificate() {
        return cert;
    }

    @Override
    public String toString() {
        return "CACertificate: " + keyname;
    }

    static {
        try {
            update();
            try (GigiPreparedStatement q = new GigiPreparedStatement("SELECT `id` FROM `cacerts`", true)) {
                GigiResultSet res = q.executeQuery();
                res.last();
                CACertificate[] certs = new CACertificate[res.getRow()];
                res.beforeFirst();
                int i = 0;
                while (res.next()) {
                    certs[i++] = getById(res.getInt(1));
                }
                instances = certs;
            }
        } catch (CertificateException e) {
            throw new Error(e);
        } catch (FileNotFoundException e) {
            throw new Error(e);
        }
    }

    private static void update() throws CertificateException, FileNotFoundException {
        File scandir = new File("config/ca");
        CertificateFactory xf = CertificateFactory.getInstance("X509");
        HashMap<X500Principal, X509Certificate> map = new HashMap<>();
        HashMap<X500Principal, String> names = new HashMap<>();
        File[] scandirfiles = scandir.listFiles();
        if (null == scandirfiles) {
            scandirfiles = new File[0];
        }
        for (File f : scandirfiles) {
            X509Certificate cert = (X509Certificate) xf.generateCertificate(new FileInputStream(f));
            X500Principal princip = cert.getSubjectX500Principal();
            map.put(princip, cert);
            String name = f.getName();
            names.put(princip, name.substring(0, name.length() - 4));
        }
        HashMap<X500Principal, Integer> inserted = new HashMap<>();
        for (X509Certificate i : map.values()) {
            if (inserted.containsKey(i.getSubjectX500Principal())) {
                continue;
            }
            Deque<X509Certificate> toInserts = new ArrayDeque<>();
            toInserts.add(i);
            while ( !inserted.containsKey(i.getIssuerX500Principal()) && !i.getIssuerX500Principal().equals(i.getSubjectX500Principal())) {
                i = map.get(i.getIssuerX500Principal());
                toInserts.addFirst(i);
            }
            for (X509Certificate toInsert : toInserts) {

                X500Principal subj = toInsert.getSubjectX500Principal();
                boolean self = toInsert.getIssuerX500Principal().equals(subj);
                try (GigiPreparedStatement q = new GigiPreparedStatement("SELECT `id`, `parentRoot` FROM `cacerts` WHERE `keyname`=?")) {
                    q.setString(1, names.get(subj));
                    GigiResultSet res = q.executeQuery();
                    int id;
                    if (res.next()) {
                        id = res.getInt("id");
                        if (res.getInt("parentRoot") != (self ? id : inserted.get(toInsert.getIssuerX500Principal()))) {
                            throw new Error("Invalid DB structure: " + subj + "->" + inserted.get(toInsert.getIssuerX500Principal()) + " vs " + res.getInt("parentRoot"));
                        }
                    } else {
                        String link;
                        String keyname = names.get(subj);
                        if ( !keyname.contains("_")) {
                            link = "https://" + ServerConstants.getHostNamePortSecure(Host.CRT_REPO) + "/g2/" + keyname + ".crt";
                        } else {
                            String[] parts = keyname.split("_");
                            link = "https://" + ServerConstants.getHostNamePortSecure(Host.CRT_REPO) + "/g2/" + parts[1] + "/" + parts[0] + "-" + parts[2] + ".crt";

                        }
                        try (GigiPreparedStatement q2 = new GigiPreparedStatement("INSERT INTO `cacerts` SET `parentRoot`=?, `keyname`=?, `link`=?")) {
                            q2.setInt(1, self ? 0 : inserted.get(toInsert.getIssuerX500Principal()));
                            q2.setString(2, keyname);
                            q2.setString(3, link);
                            q2.execute();
                            id = q2.lastInsertId();
                        }
                        if (self) {
                            try (GigiPreparedStatement q3 = new GigiPreparedStatement("UPDATE `cacerts` SET `parentRoot`=? WHERE `id`=?")) {
                                q3.setInt(1, id);
                                q3.setInt(2, id);
                                q3.execute();
                            }
                        }
                    }
                    inserted.put(subj, id);
                }
            }
        }
    }

    @Override
    public int getId() {
        return id;
    }

    public String getKeyname() {
        return keyname;
    }

    public String getLink() {
        return link;
    }

    public static synchronized CACertificate getById(int id) throws IllegalArgumentException {
        CACertificate em = myCache.get(id);
        if (em == null) {
            myCache.put(em = new CACertificate(id));
        }
        return em;
    }

    public boolean isSelfsigned() {
        return this == getParent();
    }

    public static synchronized CACertificate[] getAll() {
        return Arrays.copyOf(instances, instances.length);
    }

}
