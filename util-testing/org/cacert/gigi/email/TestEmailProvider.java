package org.cacert.gigi.email;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.cert.Certificate;
import java.util.Properties;

/**
 * This class intercepts emails so that the test cases can evaluate them
 * automatically.
 */
public class TestEmailProvider extends EmailProvider {

    private ServerSocket servs;

    private Socket client;

    private DataOutputStream out;

    private DataInputStream in;

    private EmailProvider target;

    protected TestEmailProvider(Properties props) {
        try {
            String name = props.getProperty("emailProvider.test.target");
            if (name != null) {
                Class<?> c = Class.forName(name);
                target = (EmailProvider) c.getDeclaredConstructor(Properties.class).newInstance(props);
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            servs = new ServerSocket(Integer.parseInt(props.getProperty("emailProvider.port")), 10, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void sendMail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        while (true) {
            if ( !assureLocalConnection() && target != null) {
                target.sendMail(to, subject, message, from, replyto, toname, fromname, errorsto, extra);
                return;
            }
            try {
                if (out == null) {
                    continue;
                }
                out.writeUTF("mail");
                write(to);
                write(subject);
                write(message);
                write(from);
                write(replyto);
                out.flush();
                return;
            } catch (IOException e) {
                client = null;
            }
        }
    }

    private boolean assureLocalConnection() throws IOException {
        if (out != null) {
            try {
                out.writeUTF("ping");
            } catch (IOException e) {
                client = null;
            }
        }
        if (client == null || client.isClosed()) {
            servs.setSoTimeout(2000);
            try {
                client = servs.accept();
            } catch (SocketTimeoutException e) {
                return false;
            }
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
        }
        return true;
    }

    @Override
    public synchronized String checkEmailServer(int forUid, String address) throws IOException {
        while (true) {
            if ( !assureLocalConnection() && target != null) {
                return target.checkEmailServer(forUid, address);
            }
            try {
                out.writeUTF("challengeAddrBox");
                out.writeUTF(address);
                return in.readUTF();
            } catch (IOException e) {
                client = null;
            }
        }
    }

    private void write(String to) throws IOException {
        if (to == null) {
            out.writeUTF("<null>");
        } else {
            out.writeUTF(to);
        }
    }

    @Override
    protected void init(Certificate c, Key k) {
        super.init(c, k);
        if (target != null) {
            target.init(c, k);
        }
    }
}
