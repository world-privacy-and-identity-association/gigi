package org.cacert.gigi.email;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

    protected TestEmailProvider(Properties props) {
        try {
            servs = new ServerSocket(Integer.parseInt(props.getProperty("emailProvider.port")), 10, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
        while (true) {
            assureLocalConnection();
            try {
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

    private void assureLocalConnection() throws IOException {
        if (out != null) {
            try {
                out.writeUTF("ping");
            } catch (IOException e) {
                client = null;
            }
        }
        if (client == null || client.isClosed()) {
            client = servs.accept();
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
        }
    }

    @Override
    public synchronized String checkEmailServer(int forUid, String address) throws IOException {
        while (true) {
            assureLocalConnection();
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
