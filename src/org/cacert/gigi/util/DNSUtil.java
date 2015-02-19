package org.cacert.gigi.util;

import java.util.Arrays;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

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

        Attributes dnsLookup = context.getAttributes(name, new String[] {
            "TXT"
        });
        context.close();

        return extractTextEntries(dnsLookup.get("TXT"));
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

    public static void main(String[] args) throws NamingException {
        if (args[0].equals("MX")) {
            System.out.println(Arrays.toString(getMXEntries(args[1])));
        } else if (args[0].equals("NS")) {
            System.out.println(Arrays.toString(getNSNames(args[1])));
        } else if (args[0].equals("TXT")) {
            System.out.println(Arrays.toString(getTXTEntries(args[1], args[2])));
        }
    }

}
