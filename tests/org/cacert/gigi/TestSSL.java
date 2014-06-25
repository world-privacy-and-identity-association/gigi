package org.cacert.gigi;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import org.cacert.gigi.testUtils.ManagedTest;
import org.junit.Test;

public class TestSSL extends ManagedTest {
	private ByteBuffer in;
	private ByteBuffer inC;
	private ByteBuffer outC;
	private ByteBuffer out;
	static {
		InitTruststore.run();
	}
	@Test
	public void testClientIntitiatedRenegotiation()
			throws NoSuchAlgorithmException, IOException {
		SSLContext sc = SSLContext.getDefault();
		SSLEngine se = sc.createSSLEngine();
		String[] serverParts = getServerName().split(":", 2);
		SocketChannel s = SocketChannel.open(new InetSocketAddress(
				serverParts[0], Integer.parseInt(serverParts[1])));

		in = ByteBuffer.allocate(se.getSession().getApplicationBufferSize());
		inC = ByteBuffer.allocate(se.getSession().getPacketBufferSize());
		inC.limit(0);
		out = ByteBuffer.allocate(se.getSession().getApplicationBufferSize());
		outC = ByteBuffer.allocate(se.getSession().getPacketBufferSize());
		outC.limit(0);
		se.setUseClientMode(true);
		se.beginHandshake();

		work(se, s);
		se.beginHandshake();
		try {
			work(se, s);
			throw new Error(
					"Client re-negotiation succeded (possible DoS vulnerability");
		} catch (EOFException e) {
			// Cool, server closed connection
		}

	}
	private void work(SSLEngine se, SocketChannel s) throws SSLException,
			IOException {
		while (se.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING
				&& se.getHandshakeStatus() != HandshakeStatus.FINISHED) {
			switch (se.getHandshakeStatus()) {
				case NEED_WRAP :
					wrap(se, s);
					break;
				case NEED_UNWRAP :
					unwrap(se, s);
					break;
				case NEED_TASK :
					se.getDelegatedTask().run();
					break;
				default :
					System.out.println(se.getHandshakeStatus());
			}
		}
	}
	private SSLEngineResult unwrap(SSLEngine se, SocketChannel s)
			throws IOException, SSLException {
		if (inC.remaining() == 0) {
			inC.clear();
			s.read(inC);
			inC.flip();
		}
		SSLEngineResult result = se.unwrap(inC, in);
		if (result.getStatus() == javax.net.ssl.SSLEngineResult.Status.BUFFER_UNDERFLOW) {
			int pos = inC.position();
			int limit = inC.limit();
			inC.limit(inC.capacity());
			inC.position(limit);
			int read = s.read(inC);
			if (read <= 0) {
				throw new EOFException();
			}
			inC.limit(inC.position());
			inC.position(pos);
		}
		return result;
	}
	private SSLEngineResult wrap(SSLEngine se, SocketChannel s)
			throws SSLException, IOException {
		outC.clear();
		SSLEngineResult result = se.wrap(out, outC);
		outC.flip();
		s.write(outC);

		return result;
	}
}
