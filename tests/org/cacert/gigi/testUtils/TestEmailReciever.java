package org.cacert.gigi.testUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.email.EmailProvider;

public class TestEmailReciever extends EmailProvider implements Runnable {

    public class TestMail {

        String to;

        String subject;

        String message;

        String from;

        String replyto;

        public TestMail(String to, String subject, String message, String from, String replyto) {
            this.to = to;
            this.subject = subject;
            this.message = message;
            this.from = from;
            this.replyto = replyto;
        }

        public String getTo() {
            return to;
        }

        public String getSubject() {
            return subject;
        }

        public String getMessage() {
            return message;
        }

        public String getFrom() {
            return from;
        }

        public String getReplyto() {
            return replyto;
        }

        public String extractLink() {
            Pattern link = Pattern.compile("https?://[^\\s]+(?=\\s)");
            Matcher m = link.matcher(getMessage());
            m.find();
            return m.group(0);
        }

        public void verify() throws IOException {
            String[] parts = extractLink().split("\\?");
            URL u = new URL("https://" + ManagedTest.getServerName() + "/verify?" + parts[1]);

            URLConnection csrfConn = u.openConnection();
            String csrf = ManagedTest.getCSRF(csrfConn, 0);

            u = new URL("https://" + ManagedTest.getServerName() + "/verify");
            URLConnection uc = u.openConnection();
            ManagedTest.cookie(uc, ManagedTest.stripCookie(csrfConn.getHeaderField("Set-Cookie")));
            uc.setDoOutput(true);
            uc.getOutputStream().write((parts[1] + "&csrf=" + csrf).getBytes("UTF-8"));
            uc.connect();
            uc.getInputStream().close();
        }

    }

    private Socket s;

    private DataInputStream dis;

    private DataOutputStream dos;

    public TestEmailReciever(SocketAddress target) throws IOException {
        s = new Socket();
        s.connect(target);
        s.setKeepAlive(true);
        s.setSoTimeout(1000 * 60 * 60);
        dis = new DataInputStream(s.getInputStream());
        dos = new DataOutputStream(s.getOutputStream());
        new Thread(this).start();
        setInstance(this);
    }

    LinkedBlockingQueue<TestMail> mails = new LinkedBlockingQueue<TestEmailReciever.TestMail>();

    public TestMail recieve() throws InterruptedException {
        return mails.poll(5, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String type = dis.readUTF();
                if (type.equals("mail")) {
                    String to = dis.readUTF();
                    String subject = dis.readUTF();
                    String message = dis.readUTF();
                    String from = dis.readUTF();
                    String replyto = dis.readUTF();
                    mails.add(new TestMail(to, subject, message, from, replyto));
                } else if (type.equals("challengeAddrBox")) {
                    String email = dis.readUTF();
                    dos.writeUTF(quickEmailCheck(email));
                    dos.flush();
                } else if (type.equals("ping")) {
                } else {
                    System.err.println("Unknown type: " + type);
                }
            }
        } catch (IOException e) {
            if ( !closed) {
                e.printStackTrace();
            }
        }

    }

    private String quickEmailCheck(String email) throws IOException {
        if (approveRegex.matcher(email).matches()) {
            return "OK";
        } else {
            return error;
        }
    }

    String error = "FAIL";

    public void setEmailCheckError(String error) {
        this.error = error;
    }

    Pattern approveRegex = Pattern.compile(".*");

    public void setApproveRegex(Pattern approveRegex) {
        this.approveRegex = approveRegex;
    }

    public void clearMails() {
        mails.clear();
    }

    public void reset() {
        clearMails();
        error = "FAIL";
        approveRegex = Pattern.compile(".*");
    }

    boolean closed = false;

    public void destroy() {
        try {
            closed = true;
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String checkEmailServer(int forUid, String address) throws IOException {
        return quickEmailCheck(address);
    }

    @Override
    public void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        mails.add(new TestMail(to, subject, message, from, replyto));
    }

}
