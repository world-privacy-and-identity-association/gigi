package club.wpia.gigi.util;

import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import club.wpia.gigi.util.CAA.CAARecord;

public class DNSUtil {

    private static InitialDirContext context;
    static {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        try {
            context = new InitialDirContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
        }

    }

    public static String[] getNSNames(String name) throws NamingException {
        Attributes dnsLookup = context.getAttributes(name, new String[] {
                "NS"
        });
        return extractTextEntries(dnsLookup.get("NS"));
    }

    public static String[] getTXTEntries(String name, String server) throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.AUTHORITATIVE, "true");
        env.put(Context.PROVIDER_URL, "dns://" + server);

        InitialDirContext context = new InitialDirContext(env);
        try {
            Attributes dnsLookup = context.getAttributes(name, new String[] {
                    "TXT"
            });

            return extractTextEntries(dnsLookup.get("TXT"));
        } finally {
            context.close();
        }
    }

    private static String[] extractTextEntries(Attribute nsRecords) throws NamingException {
        if (nsRecords == null) {
            return new String[] {};
        }
        String[] result = new String[nsRecords.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (String) nsRecords.get(i);
        }
        return result;
    }

    public static String[] getMXEntries(String domain) throws NamingException {
        Attributes dnsLookup = context.getAttributes(domain, new String[] {
                "MX"
        });
        return extractTextEntries(dnsLookup.get("MX"));
    }

    public static CAARecord[] getCAAEntries(String domain) throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        InitialDirContext context = new InitialDirContext(env);
        try {
            Attributes dnsLookup;
            try {
                dnsLookup = context.getAttributes(domain, new String[] {
                    "257"
                });
            } catch (NameNotFoundException e) {
                // We treat non-existing names as names without CAA-records
                return new CAARecord[0];
            }

            Attribute nsRecords = dnsLookup.get("257");
            if (nsRecords == null) {
                return new CAARecord[] {};
            }

            CAA.CAARecord[] result = new CAA.CAARecord[nsRecords.size()];
            for (int i = 0; i < result.length; i++) {
                byte[] rec = (byte[]) nsRecords.get(i);

                result[i] = new CAA.CAARecord(rec);
            }

            return result;
        } finally {
            context.close();
        }
    }

    public static void main(String[] args) throws NamingException {
        if (args[0].equals("MX")) {
            System.out.println(Arrays.toString(getMXEntries(args[1])));
        } else if (args[0].equals("NS")) {
            System.out.println(Arrays.toString(getNSNames(args[1])));
        } else if (args[0].equals("TXT")) {
            System.out.println(Arrays.toString(getTXTEntries(args[1], args[2])));
        } else if (args[0].equals("CAA")) {
            System.out.println(Arrays.toString(getCAAEntries(args[1])));
        }
    }

}
