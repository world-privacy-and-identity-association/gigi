package org.cacert.gigi.email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.net.ssl.SSLSocketFactory;

import org.cacert.gigi.crypto.SMIME;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.util.DNSUtil;
import org.cacert.gigi.util.DomainAssessment;

public abstract class EmailProvider {

    public abstract void sendMail(String to, String subject, String message, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException;

    private static EmailProvider instance;

    private X509Certificate c;

    private PrivateKey k;

    protected void init(Certificate c, Key k) {
        this.c = (X509Certificate) c;
        this.k = (PrivateKey) k;
    }

    protected final void sendSigned(String contents, PrintWriter output) throws IOException, GeneralSecurityException {
        if (k == null || c == null) {
            output.println("Content-Transfer-Encoding: base64");
            output.println();
            output.print(contents);
        } else {
            SMIME.smime(contents, k, c, output);
        }
    }

    public static EmailProvider getInstance() {
        return instance;
    }

    protected static void setInstance(EmailProvider instance) {
        EmailProvider.instance = instance;
    }

    public static void initSystem(Properties conf, Certificate cert, Key pk) {
        try {
            Class<?> c = Class.forName(conf.getProperty("emailProvider"));
            EmailProvider ep = (EmailProvider) c.getDeclaredConstructor(Properties.class).newInstance(conf);
            ep.init(cert, pk);
            instance = ep;
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static final String OK = "OK";

    public static final String FAIL = "FAIL";

    private static final String MAIL_P_RFC_WORD = "[A-Za-z0-9\\+\\.!#$%&'*/=?^_`|~{}-]+";

    private static final String MAIL_P_RFC_LOCAL = MAIL_P_RFC_WORD + "(?:\\." + MAIL_P_RFC_WORD + ")*";

    private static final String MAIL_P_RFC_LABEL = "(?!(?!xn)..--|-)(?:[A-Za-z0-9-]+)(?<!-)";

    private static final String MAIL_P_RFC_ADDRESS = MAIL_P_RFC_LOCAL + "@(?:" + MAIL_P_RFC_LABEL + "\\.)+" + MAIL_P_RFC_LABEL + "\\.?";

    private static final Pattern MAIL_LOCAL = Pattern.compile("^" + MAIL_P_RFC_LOCAL + "$");

    private static final Pattern MAIL_ADDRESS = Pattern.compile("^" + MAIL_P_RFC_ADDRESS + "$");

    public String checkEmailServer(int forUid, String address) throws IOException {
        if (isValidMailAddress(address)) {
            String[] parts = address.split("@", 2);
            String domain = parts[1];

            String[] mxhosts;
            try {
                mxhosts = DNSUtil.getMXEntries(domain);
            } catch (NamingException e1) {
                return "MX lookup for your hostname failed.";
            }
            sortMX(mxhosts);

            for (String host : mxhosts) {
                host = host.split(" ", 2)[1];
                if (host.endsWith(".")) {
                    host = host.substring(0, host.length() - 1);
                } else {
                    return "Strange MX records.";
                }
                try (Socket s = new Socket(host, 25);
                        BufferedReader br0 = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));//
                        PrintWriter pw0 = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"))) {
                    BufferedReader br = br0;
                    PrintWriter pw = pw0;
                    String line;
                    if ( !SendMail.readSMTPResponse(br, 220)) {
                        continue;
                    }

                    pw.print("EHLO www.cacert.org\r\n");
                    pw.flush();
                    boolean starttls = false;
                    do {
                        line = br.readLine();
                        if (line == null) {
                            break;
                        }
                        starttls |= line.substring(4).equals("STARTTLS");
                    } while (line.startsWith("250-"));
                    if (line == null || !line.startsWith("250 ")) {
                        continue;
                    }

                    if (starttls) {
                        pw.print("STARTTLS\r\n");
                        pw.flush();
                        if ( !SendMail.readSMTPResponse(br, 220)) {
                            continue;
                        }
                        Socket s1 = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, 25, true);
                        br = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
                        pw = new PrintWriter(new OutputStreamWriter(s1.getOutputStream(), "UTF-8"));
                        pw.print("EHLO www.cacert.org\r\n");
                        pw.flush();
                        if ( !SendMail.readSMTPResponse(br, 250)) {
                            continue;
                        }
                    }

                    pw.print("MAIL FROM: <returns@cacert.org>\r\n");
                    pw.flush();

                    if ( !SendMail.readSMTPResponse(br, 250)) {
                        continue;
                    }
                    pw.print("RCPT TO: <" + address + ">\r\n");
                    pw.flush();

                    if ( !SendMail.readSMTPResponse(br, 250)) {
                        continue;
                    }
                    pw.print("QUIT\r\n");
                    pw.flush();
                    if ( !SendMail.readSMTPResponse(br, 221)) {
                        continue;
                    }

                    try (GigiPreparedStatement statmt = new GigiPreparedStatement("INSERT INTO `emailPinglog` SET `when`=NOW(), `email`=?, `result`=?, `uid`=?, `type`='fast', `status`=?::`pingState`")) {
                        statmt.setString(1, address);
                        statmt.setString(2, line);
                        statmt.setInt(3, forUid);
                        statmt.setString(4, "success");
                        statmt.execute();
                    }

                    if (line == null || !line.startsWith("250")) {
                        return line;
                    } else {
                        return OK;
                    }
                }

            }
        }
        try (GigiPreparedStatement statmt = new GigiPreparedStatement("INSERT INTO `emailPinglog` SET `when`=NOW(), `email`=?, `result`=?, `uid`=?, `type`='fast', `status`=?::`pingState`")) {
            statmt.setString(1, address);
            statmt.setString(2, "Failed to make a connection to the mail server");
            statmt.setInt(3, forUid);
            statmt.setString(4, "failed");
            statmt.execute();
        }
        return FAIL;
    }

    private static void sortMX(String[] mxhosts) {
        Arrays.sort(mxhosts, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                int i1 = Integer.parseInt(o1.split(" ")[0]);
                int i2 = Integer.parseInt(o2.split(" ")[0]);
                return Integer.compare(i1, i2);
            }
        });
    }

    public static boolean isValidMailAddress(String address) {
        if ( !MAIL_ADDRESS.matcher(address).matches()) {
            return false;
        }

        String[] parts = address.split("@", 2);

        String local = parts[0];
        String domain = parts[1];

        if ( !MAIL_LOCAL.matcher(local).matches()) {
            return false;
        }

        for (String domainPart : domain.split("\\.", -1)) {
            if ( !DomainAssessment.isValidDomainPart(domainPart)) {
                return false;
            }
        }

        return true;
    }

}
