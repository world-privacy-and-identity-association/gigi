package org.cacert.gigi.email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import org.cacert.gigi.util.PEM;
import org.cacert.gigi.util.ServerConstants;

public class Sendmail extends EmailProvider {

    protected Sendmail(Properties props) {}

    private static final Pattern NON_ASCII = Pattern.compile("[^a-zA-Z0-9 .-\\[\\]!_@]");

    @Override
    public void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {

        String[] bits = from.split(",");

        try (Socket smtp = new Socket("localhost", 25); PrintWriter out = new PrintWriter(new OutputStreamWriter(smtp.getOutputStream(), "UTF-8")); BufferedReader in = new BufferedReader(new InputStreamReader(smtp.getInputStream(), "UTF-8"));) {
            readSMTPResponse(in, 220);
            out.print("HELO www.cacert.org\r\n");
            out.flush();
            readSMTPResponse(in, 250);
            out.print("MAIL FROM:<returns@cacert.org>\r\n");
            out.flush();
            readSMTPResponse(in, 250);
            bits = to.split(",");
            for (String user : bits) {
                out.print("RCPT TO:<" + user.trim() + ">\r\n");
                out.flush();
                readSMTPResponse(in, 250);
            }
            out.print("DATA\r\n");
            out.flush();
            readSMTPResponse(in, 250);
            out.print("X-Mailer: CAcert.org Website\r\n");
            // if (array_key_exists("REMOTE_ADDR", $_SERVER)) {
            // out.print("X-OriginatingIP: ".$_SERVER["REMOTE_ADDR"]."\r\n");
            // }
            // TODO
            SimpleDateFormat emailDate = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss ZZZZ (z)", Locale.ENGLISH);
            out.print("Date: " + emailDate.format(new Date(System.currentTimeMillis())) + "\r\n");
            if (errorsto != null) {
                out.print("Sender: " + errorsto + "\r\n");
                out.print("Errors-To: " + errorsto + "\r\n");
            }
            if (replyto != null) {
                out.print("Reply-To: " + replyto + "\r\n");
            } else {
                out.print("Reply-To: " + from + "\r\n");
            }
            out.print("From: support@" + ServerConstants.getWwwHostName().replaceAll("^www.", "") + "\r\n");
            out.print("To: " + to + "\r\n");
            if (NON_ASCII.matcher(subject).matches()) {

                out.print("Subject: =?utf-8?B?" + Base64.getEncoder().encodeToString(subject.getBytes("UTF-8")) + "?=\r\n");
            } else {
                out.print("Subject: " + subject + "\r\n");
            }
            StringBuffer headers = new StringBuffer();
            headers.append("Content-Type: text/plain; charset=\"utf-8\"\r\n");
            headers.append("Content-Transfer-Encoding: base64\r\n");
            // out.print(chunk_split(base64_encode(recode("html..utf-8",
            // $message)))."\r\n.\r\n");
            headers.append("\r\n");
            headers.append(PEM.formatBase64(message.getBytes("UTF-8")));
            headers.append("\r\n");

            try {
                sendSigned(headers.toString(), out);
                out.print("\r\n.\r\n");
                out.flush();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return;
            }
            readSMTPResponse(in, 250);
            out.print("QUIT\n");
            out.flush();
            readSMTPResponse(in, 221);
        }
    }

    public static boolean readSMTPResponse(BufferedReader in, int code) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith(code + " ")) {
                return true;
            } else if ( !line.startsWith(code + "-")) {
                return false;
            }
        }
        return false;

    }

}
