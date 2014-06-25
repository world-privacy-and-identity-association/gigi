package org.cacert.gigi.email;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

class TestEmailProvider extends EmailProvider {
	ServerSocket servs;
	Socket client;
	DataOutputStream out;
	DataInputStream in;
	protected TestEmailProvider(Properties props) {
		try {
			servs = new ServerSocket(Integer.parseInt(props
					.getProperty("emailProvider.port")), 10,
					InetAddress.getByName("127.0.0.1"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public synchronized void sendmail(String to, String subject,
			String message, String from, String replyto, String toname,
			String fromname, String errorsto, boolean extra) throws IOException {
		boolean sent = false;
		while (!sent) {
			if (client == null || client.isClosed()) {
				client = servs.accept();
				out = new DataOutputStream(client.getOutputStream());
				in = new DataInputStream(client.getInputStream());
			}
			try {
				out.writeUTF("mail");
				write(to);
				write(subject);
				write(message);
				write(from);
				write(replyto);
				out.flush();
				sent = true;
			} catch (IOException e) {
				client = null;
			}
		}
	}
	@Override
	public String checkEmailServer(int forUid, String address)
			throws IOException {
		out.writeUTF("challengeAddrBox");
		out.writeUTF(address);
		return in.readUTF();
	}

	private void write(String to) throws IOException {
		if (to == null) {
			out.writeUTF("<null>");
		} else {
			out.writeUTF(to);
		}
	}

}
