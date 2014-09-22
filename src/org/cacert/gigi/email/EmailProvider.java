package org.cacert.gigi.email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.cacert.gigi.crypto.SMIME;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.util.DNSUtil;

public abstract class EmailProvider {

    public abstract void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException;

    private static EmailProvider instance;

    private X509Certificate c;

    private PrivateKey k;

    protected final void init(Certificate c, Key k) {
        this.c = (X509Certificate) c;
        this.k = (PrivateKey) k;
    }

    protected final void sendSigned(String contents, PrintWriter output) throws IOException, GeneralSecurityException {
        SMIME.smime(contents, k, c, output);
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

            for (String host : mxhosts) {
                host = host.split(" ", 2)[1];
                if (host.endsWith(".")) {
                    host = host.substring(0, host.length() - 1);
                } else {
                    return "Strange MX records.";
                }
                try (Socket s = new Socket(host, 25); BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); PrintWriter pw = new PrintWriter(s.getOutputStream())) {
                    String line;
                    while ((line = br.readLine()) != null && line.startsWith("220-")) {
                    }
                    if (line == null || !line.startsWith("220")) {
                        continue;
                    }

                    pw.print("HELO www.cacert.org\r\n");
                    pw.flush();

                    while ((line = br.readLine()) != null && line.startsWith("220")) {
                    }

                    if (line == null || !line.startsWith("250")) {
                        continue;
                    }
                    pw.print("MAIL FROM: <returns@cacert.org>\r\n");
                    pw.flush();

                    line = br.readLine();

                    if (line == null || !line.startsWith("250")) {
                        continue;
                    }
                    pw.print("RCPT TO: <" + address + ">\r\n");
                    pw.flush();

                    line = br.readLine();
                    pw.print("QUIT\r\n");
                    pw.flush();

                    GigiPreparedStatement statmt = DatabaseConnection.getInstance().prepare("insert into `pinglog` set `when`=NOW(), `email`=?, `result`=?, `uid`=?");
                    statmt.setString(1, address);
                    statmt.setString(2, line);
                    statmt.setInt(3, forUid);
                    statmt.execute();

                    if (line == null || !line.startsWith("250")) {
                        return line;
                    } else {
                        return OK;
                    }
                }

            }
        }
        GigiPreparedStatement statmt = DatabaseConnection.getInstance().prepare("insert into `pinglog` set `when`=NOW(), `email`=?, `result`=?, `uid`=?");
        statmt.setString(1, address);
        statmt.setString(2, "Failed to make a connection to the mail server");
        statmt.setInt(3, forUid);
        statmt.execute();
        return FAIL;
    }

}
