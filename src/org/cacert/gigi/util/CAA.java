package org.cacert.gigi.util;

import javax.naming.NamingException;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.CertificateProfile;
import org.cacert.gigi.output.template.SprintfCommand;

public class CAA {

    public static class CAARecord {

        private byte flags;

        private String tag;

        private String data;

        public CAARecord(byte[] rec) {
            byte length = (byte) (rec[1] & 0xFF);
            tag = new String(rec, 2, length);
            data = new String(rec, 2 + length, rec.length - 2 - length);
            flags = rec[0];
        }

        @Override
        public String toString() {
            return "CAA " + (flags & 0xFF) + " " + tag + " " + data;
        }

        public String getData() {
            return data;
        }

        public byte getFlags() {
            return flags;
        }

        public String getTag() {
            return tag;
        }

        public boolean isCritical() {
            return (flags & (byte) 0x80) == (byte) 0x80;
        }
    }

    public static boolean verifyDomainAccess(CertificateOwner owner, CertificateProfile p, String name) throws GigiApiException {
        try {
            if (name.startsWith("*.")) {
                return verifyDomainAccess(owner, p, name.substring(2), true);
            }
            return verifyDomainAccess(owner, p, name, false);
        } catch (NamingException e) {
            throw new GigiApiException(SprintfCommand.createSimple("Internal Name Server/Resolution Error: {0}", e.getMessage()));
        }
    }

    private static boolean verifyDomainAccess(CertificateOwner owner, CertificateProfile p, String name, boolean wild) throws NamingException {
        CAARecord[] caa = getEffectiveCAARecords(name);
        if (caa.length == 0) {
            return true; // default assessment is beeing granted
        }
        for (int i = 0; i < caa.length; i++) {
            CAARecord r = caa[i];
            if (r.getTag().equals("issuewild")) {
                if (wild && authorized(owner, p, r.getData())) {
                    return true;
                }
            } else if (r.getTag().equals("iodef")) {
                // TODO send mail/form
            } else if (r.getTag().equals("issue")) {
                if ( !wild && authorized(owner, p, r.getData())) {
                    return true;
                }
            } else {
                if (r.isCritical()) {
                    return false; // found critical, unkown entry
                }
                // ignore unkown tags
            }
        }
        return false;
    }

    private static CAARecord[] getEffectiveCAARecords(String name) throws NamingException {
        CAARecord[] caa = DNSUtil.getCAAEntries(name);
        String publicSuffix = PublicSuffixes.getInstance().getRegistrablePart(name);
        // TODO missing alias processing
        while (caa.length == 0 && name.contains(".")) {
            name = name.split("\\.", 2)[1];
            caa = DNSUtil.getCAAEntries(name);
            if (name.equals(publicSuffix)) {
                return caa;
            }
        }
        return caa;
    }

    private static boolean authorized(CertificateOwner owner, CertificateProfile p, String data) {
        String[] parts = data.split(";");
        String ca = parts[0].trim();
        if ( !ca.equals("cacert.org")) {
            return false;
        }
        for (int i = 1; i < parts.length; i++) {
            String[] pa = parts[i].split("=");
            String key = pa[0].trim();
            String v = pa[1].trim();
            if (key.equals("account")) {
                int id = Integer.parseInt(v);
                if (id != owner.getId()) {
                    return false;
                }
            } else { // unknown key... be conservative
                return false;
            }
        }
        return true;
    }

}
