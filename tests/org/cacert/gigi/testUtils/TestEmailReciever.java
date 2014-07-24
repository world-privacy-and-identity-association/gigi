package org.cacert.gigi.testUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestEmailReciever implements Runnable {
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
					if (approveRegex.matcher(email).matches()) {
						dos.writeUTF("OK");
					} else {
						dos.writeUTF(error);
					}
					dos.flush();
				} else if (type.equals("ping")) {
				} else {
					System.err.println("Unknown type: " + type);
				}
			}
		} catch (IOException e) {
			if (!closed) {
				e.printStackTrace();
			}
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

}
