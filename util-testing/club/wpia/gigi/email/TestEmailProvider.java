package club.wpia.gigi.email;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

/**
 * This class intercepts emails so that the test cases can evaluate them
 * automatically.
 */
public class TestEmailProvider extends DelegateMailProvider {

    private ServerSocket servs;

    private Socket client;

    private DataOutputStream out;

    private DataInputStream in;

    protected TestEmailProvider(Properties props) {
        super(props, props.getProperty("emailProvider.test.target"));
        try {
            servs = new ServerSocket(Integer.parseInt(props.getProperty("emailProvider.port")), 10, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void sendMail(String to, String subject, String message, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        while (true) {
            if ( !ensureLocalConnection() && getTarget() != null) {
                super.sendMail(to, subject, message, replyto, toname, fromname, errorsto, extra);
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
                write(replyto);
                out.flush();
                return;
            } catch (IOException e) {
                client = null;
            }
        }
    }

    private boolean ensureLocalConnection() throws IOException {
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
            if ( !ensureLocalConnection() && getTarget() != null) {
                return super.checkEmailServer(forUid, address);
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
}
