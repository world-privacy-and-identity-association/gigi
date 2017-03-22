package club.wpia.gigi.email;

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

import club.wpia.gigi.crypto.SMIME;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.util.DNSUtil;
import club.wpia.gigi.util.DomainAssessment;
import club.wpia.gigi.util.SystemKeywords;

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

    public String checkEmailServer(int forUid, final String address) throws IOException {
        if ( !isValidMailAddress(address)) {
            try (GigiPreparedStatement statmt = new GigiPreparedStatement("INSERT INTO `emailPinglog` SET `when`=NOW(), `email`=?, `result`=?, `uid`=?, `type`='fast'::`emailPingType`, `status`='failed'::`pingState`")) {
                statmt.setString(1, address);
                statmt.setString(2, "Invalid email address provided");
                statmt.setInt(3, forUid);
                statmt.execute();
            }
            return FAIL;
        }

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

            class SMTPSessionHandler {

                public boolean detectedSTARTTLS = false;

                public boolean initiateSMTPSession(BufferedReader r, PrintWriter w) throws IOException {
                    String line;

                    if ( !SendMail.readSMTPResponse(r, 220)) {
                        return false;
                    }

                    w.print("EHLO " + SystemKeywords.SMTP_NAME + "\r\n");
                    w.flush();

                    detectedSTARTTLS = false;
                    do {
                        line = r.readLine();
                        if (line == null) {
                            break;
                        }
                        detectedSTARTTLS |= line.substring(4).equals("STARTTLS");
                    } while (line.startsWith("250-"));

                    if (line == null || !line.startsWith("250 ")) {
                        return false;
                    }

                    return true;
                }

                public boolean trySendEmail(BufferedReader r, PrintWriter w) throws IOException {
                    w.print("MAIL FROM: <" + SystemKeywords.SMTP_PSEUDO_FROM + ">\r\n");
                    w.flush();

                    if ( !SendMail.readSMTPResponse(r, 250)) {
                        return false;
                    }

                    w.print("RCPT TO: <" + address + ">\r\n");
                    w.flush();

                    if ( !SendMail.readSMTPResponse(r, 250)) {
                        return false;
                    }

                    w.print("QUIT\r\n");
                    w.flush();

                    if ( !SendMail.readSMTPResponse(r, 221)) {
                        return false;
                    }

                    return true;
                }

            }

            SMTPSessionHandler sh = new SMTPSessionHandler();

            try (Socket plainSocket = new Socket(host, 25); //
                    BufferedReader plainReader = new BufferedReader(new InputStreamReader(plainSocket.getInputStream(), "UTF-8")); //
                    PrintWriter plainWriter = new PrintWriter(new OutputStreamWriter(plainSocket.getOutputStream(), "UTF-8"))) {

                if ( !sh.initiateSMTPSession(plainReader, plainWriter)) {
                    continue;
                }

                boolean canSend = false;

                if (sh.detectedSTARTTLS) {
                    plainWriter.print("STARTTLS\r\n");
                    plainWriter.flush();

                    if ( !SendMail.readSMTPResponse(plainReader, 220)) {
                        continue;
                    }

                    try (Socket tlsSocket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(plainSocket, host, 25, true); //
                            BufferedReader tlsReader = new BufferedReader(new InputStreamReader(tlsSocket.getInputStream(), "UTF-8")); //
                            PrintWriter tlsWriter = new PrintWriter(new OutputStreamWriter(tlsSocket.getOutputStream(), "UTF-8"))) {

                        tlsWriter.print("EHLO " + SystemKeywords.SMTP_NAME + "\r\n");
                        tlsWriter.flush();

                        if ( !SendMail.readSMTPResponse(tlsReader, 250)) {
                            continue;
                        }

                        canSend = sh.trySendEmail(tlsReader, tlsWriter);
                    }
                } else {
                    canSend = sh.trySendEmail(plainReader, plainWriter);
                }

                if ( !canSend) {
                    continue;
                }

                try (GigiPreparedStatement statmt = new GigiPreparedStatement("INSERT INTO `emailPinglog` SET `when`=NOW(), `email`=?, `result`=?, `uid`=?, `type`='fast', `status`='success'::`pingState`")) {
                    statmt.setString(1, address);
                    statmt.setString(2, OK);
                    statmt.setInt(3, forUid);
                    statmt.execute();
                }

                return OK;
            }
        }

        try (GigiPreparedStatement statmt = new GigiPreparedStatement("INSERT INTO `emailPinglog` SET `when`=NOW(), `email`=?, `result`=?, `uid`=?, `type`='fast'::`emailPingType`, `status`='failed'::`pingState`")) {
            statmt.setString(1, address);
            statmt.setString(2, "Failed to make a connection to the mail server");
            statmt.setInt(3, forUid);
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
