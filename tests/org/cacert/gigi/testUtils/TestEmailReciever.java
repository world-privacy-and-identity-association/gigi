package org.cacert.gigi.testUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestEmailReciever implements Runnable {
	public class TestMail {
		String to;
		String subject;
		String message;
		String from;
		String replyto;
		public TestMail(String to, String subject, String message, String from,
				String replyto) {
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

	}
	private Socket s;
	private DataInputStream dis;

	public TestEmailReciever(SocketAddress target) throws IOException {
		s = new Socket();
		s.connect(target);
		s.setKeepAlive(true);
		s.setSoTimeout(1000 * 60 * 60);
		dis = new DataInputStream(s.getInputStream());
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
				String to = dis.readUTF();
				String subject = dis.readUTF();
				String message = dis.readUTF();
				String from = dis.readUTF();
				String replyto = dis.readUTF();
				mails.add(new TestMail(to, subject, message, from, replyto));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void clearMails() {
		mails.clear();
	}

}
