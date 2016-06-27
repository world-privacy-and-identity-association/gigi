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

public abstract class EmailProvider {

    public abstract void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException;

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

    public static final Pattern MAIL = Pattern.compile("^([a-zA-Z0-9])+([a-zA-Z0-9\\+\\._-])*@([a-zA-Z0-9_-])+([a-zA-Z0-9\\._-]+)+$");

    public String checkEmailServer(int forUid, String address) throws IOException {
        if (MAIL.matcher(address).matches()) {
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
                    if ( !Sendmail.readSMTPResponse(br, 220)) {
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
                        if ( !Sendmail.readSMTPResponse(br, 220)) {
                            continue;
                        }
                        Socket s1 = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, 25, true);
                        br = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
                        pw = new PrintWriter(new OutputStreamWriter(s1.getOutputStream(), "UTF-8"));
                        pw.print("EHLO www.cacert.org\r\n");
                        pw.flush();
                        if ( !Sendmail.readSMTPResponse(br, 250)) {
                            continue;
                        }
                    }

                    pw.print("MAIL FROM: <returns@cacert.org>\r\n");
                    pw.flush();

                    if ( !Sendmail.readSMTPResponse(br, 250)) {
                        continue;
                    }
                    pw.print("RCPT TO: <" + address + ">\r\n");
                    pw.flush();

                    if ( !Sendmail.readSMTPResponse(br, 250)) {
                        continue;
                    }
                    pw.print("QUIT\r\n");
                    pw.flush();
                    if ( !Sendmail.readSMTPResponse(br, 221)) {
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

}
