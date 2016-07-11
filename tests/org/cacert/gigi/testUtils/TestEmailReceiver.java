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
import org.cacert.gigi.email.TestEmailProvider;

/**
 * This class reveives emails from the current system under test. It is the
 * counterpart to the {@link TestEmailProvider} who is loaded into the system to
 * intercept the emails. This class resides in the VM that executes the
 * testcases and supplies the intercepted emails to the current test case.
 */
public final class TestEmailReceiver extends EmailProvider implements Runnable, MailReceiver {

    /**
     * An email that has been intercepted.
     */
    public static class TestMail {

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
            String link = extractLink();
            String[] parts = link.split("\\?");
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

    /**
     * Creates a new TestEmailReceiver based on the address where the
     * {@link TestEmailProvider} is listening. This class is only ready after
     * {@link #start()} has been called.
     * 
     * @param target
     *            the address where the {@link TestEmailProvider} is listening.
     * @throws IOException
     *             if the connection cannot be opened
     */
    public TestEmailReceiver(SocketAddress target) throws IOException {
        s = new Socket();
        s.connect(target);
        s.setKeepAlive(true);
        s.setSoTimeout(1000 * 60 * 60);
        dis = new DataInputStream(s.getInputStream());
        dos = new DataOutputStream(s.getOutputStream());
        setInstance(this);
    }

    /**
     * Spawns a new {@link Thread} that reads incoming {@link TestMail}s.
     * 
     * @see #destroy()
     */
    public void start() {
        new Thread(this, "Mail receiver").start();
    }

    private LinkedBlockingQueue<TestMail> mails = new LinkedBlockingQueue<TestEmailReceiver.TestMail>();

    /**
     * Retrieves an outgoing mail from the system. The method will return a
     * {@link TestMail} or fail.
     * 
     * @return The intercepted {@link TestMail}
     * @see #poll()
     */
    @Override
    public TestMail receive() {
        TestMail poll;

        try {
            poll = mails.poll(60, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted while receiving mails");
        }
        if (poll == null) {
            throw new AssertionError("Mail receiving timed out");
        }

        return poll;
    }

    /**
     * Retrieves an outgoing mail from the system or returns <code>null</code>
     * if there was no mail sent in 30 seconds.
     * 
     * @return The intercepted {@link TestMail} or <code>null</code> if no mail
     *         has been sent.
     * @see #receive()
     */
    public TestMail poll() {
        return mails.poll();
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

    /**
     * Sets the error that will be sent back to incoming "fast mail checks" that
     * only check for the availability of a mailbox.
     * 
     * @param error
     *            the error Massage to return in
     *            {@link EmailProvider#checkEmailServer(int, String)}
     */
    public void setEmailCheckError(String error) {
        this.error = error;
    }

    private Pattern approveRegex = Pattern.compile(".*");

    /**
     * Specifies a pattern that will be used for incoming
     * {@link EmailProvider#checkEmailServer(int, String)} calls to determine
     * whether the mailbox should exist.
     * 
     * @param approveRegex
     *            the regex that will perform the check
     */
    public void setApproveRegex(Pattern approveRegex) {
        this.approveRegex = approveRegex;
    }

    /**
     * Removes all queued mails.
     */
    @Override
    public void clearMails() {
        mails.clear();
    }

    /**
     * Resets this class to its initial state
     * 
     * @see #clearMails()
     * @see #setApproveRegex(Pattern)
     * @see #setEmailCheckError(String)
     */
    public void reset() {
        clearMails();
        error = "FAIL";
        approveRegex = Pattern.compile(".*");
    }

    private boolean closed = false;

    /**
     * stops reading for incoming messages
     * 
     * @see #start()
     */
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
    public void sendMail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        mails.add(new TestMail(to, subject, message, from, replyto));
    }

}
