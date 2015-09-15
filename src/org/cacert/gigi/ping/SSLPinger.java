package org.cacert.gigi.ping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.security.cert.X509Certificate;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;

public class SSLPinger extends DomainPinger {

    public static final String[] TYPES = new String[] {
            "xmpp", "server-xmpp", "smtp", "imap"
    };

    private KeyStore truststore;

    public SSLPinger(KeyStore truststore) {
        this.truststore = truststore;
    }

    @Override
    public String ping(Domain domain, String configuration, User u) {
        try (SocketChannel sch = SocketChannel.open()) {
            String[] parts = configuration.split(":", 2);
            sch.connect(new InetSocketAddress(domain.getSuffix(), Integer.parseInt(parts[0])));
            if (parts.length == 2) {
                switch (parts[1]) {
                case "xmpp":
                    startXMPP(sch, false, domain.getSuffix());
                    break;
                case "server-xmpp":
                    startXMPP(sch, true, domain.getSuffix());
                    break;
                case "smtp":
                    startSMTP(sch);
                    break;
                case "imap":
                    startIMAP(sch);
                    break;

                }
            }
            return test(sch, domain.getSuffix(), u);
        } catch (IOException e) {
            return "Connecton failed";
        }

    }

    private void startIMAP(SocketChannel sch) throws IOException {
        Socket s = sch.socket();
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        scanFor(is, "\n");
        os.write("ENABLE STARTTLS\r\n".getBytes("UTF-8"));
        os.flush();
        scanFor(is, "\n");
    }

    private void startXMPP(SocketChannel sch, boolean server, String domain) throws IOException {
        Socket s = sch.socket();
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        os.write(("<stream:stream to=\"" + domain + "\" xmlns=\"jabber:" + (server ? "server" : "client") + "\"" + " xmlns:stream=\"http://etherx.jabber.org/streams\" version=\"1.0\">").getBytes("UTF-8"));
        os.flush();
        os.write("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>".getBytes("UTF-8"));
        os.flush();
        scanFor(is, "<proceed");
        scanFor(is, ">");

    }

    private void scanFor(InputStream is, String scanFor) throws IOException {
        int pos = 0;
        while (pos < scanFor.length()) {
            if (is.read() == scanFor.charAt(pos)) {
                pos++;
            } else {
                pos = 0;
            }
        }
    }

    private void startSMTP(SocketChannel sch) throws IOException {
        Socket s = sch.socket();
        InputStream is = s.getInputStream();
        readSMTP(is);
        s.getOutputStream().write("EHLO ssl.pinger\r\n".getBytes("UTF-8"));
        s.getOutputStream().flush();
        readSMTP(is);
        s.getOutputStream().write("HELP\r\n".getBytes("UTF-8"));
        s.getOutputStream().flush();
        readSMTP(is);
        s.getOutputStream().write("STARTTLS\r\n".getBytes("UTF-8"));
        s.getOutputStream().flush();
        readSMTP(is);
    }

    private void readSMTP(InputStream is) throws IOException {
        int counter = 0;
        boolean finish = true;
        while (true) {
            char c = (char) is.read();
            if (counter == 3) {
                if (c == ' ') {
                    finish = true;
                } else if (c == '-') {
                    finish = false;
                } else {
                    throw new Error("Invalid smtp: " + c);
                }
            }
            if (c == '\n') {
                if (finish) {
                    return;
                }
                counter = 0;
            } else {
                counter++;
            }
        }
    }

    private String test(SocketChannel sch, String domain, User subject) {
        try {
            sch.socket().setSoTimeout(5000);
            SSLContext sc = SSLContext.getInstance("SSL");
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                tmf.init(truststore);
                sc.init(null, tmf.getTrustManagers(), new SecureRandom());
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
            SSLEngine se = sc.createSSLEngine();
            ByteBuffer enc_in = ByteBuffer.allocate(se.getSession().getPacketBufferSize());
            ByteBuffer enc_out = ByteBuffer.allocate(se.getSession().getPacketBufferSize());
            ByteBuffer dec_in = ByteBuffer.allocate(se.getSession().getApplicationBufferSize());
            ByteBuffer dec_out = ByteBuffer.allocate(se.getSession().getApplicationBufferSize());
            se.setUseClientMode(true);
            SSLParameters sp = se.getSSLParameters();
            sp.setServerNames(Arrays.<SNIServerName>asList(new SNIHostName(domain)));
            se.setSSLParameters(sp);
            se.beginHandshake();
            enc_in.limit(0);
            while (se.getHandshakeStatus() != HandshakeStatus.FINISHED && se.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
                switch (se.getHandshakeStatus()) {
                case NEED_WRAP:
                    dec_out.limit(0);
                    se.wrap(dec_out, enc_out);
                    enc_out.flip();
                    while (enc_out.remaining() > 0) {
                        sch.write(enc_out);
                    }
                    enc_out.clear();
                    break;
                case NEED_UNWRAP:
                    if (enc_in.remaining() == 0) {
                        enc_in.clear();
                        sch.read(enc_in);
                        enc_in.flip();
                    }
                    while (se.unwrap(enc_in, dec_in).getStatus() == Status.BUFFER_UNDERFLOW) {
                        enc_in.position(enc_in.limit());
                        enc_in.limit(enc_in.capacity());
                        sch.read(enc_in);
                        enc_in.flip();
                    }
                    enc_in.compact();
                    enc_in.flip();
                    break;
                case NEED_TASK:
                    se.getDelegatedTask().run();
                    break;
                case NOT_HANDSHAKING:
                case FINISHED:

                }

            }
            X509Certificate[] peerCertificateChain = se.getSession().getPeerCertificateChain();
            X509Certificate first = peerCertificateChain[0];

            BigInteger serial = first.getSerialNumber();
            Certificate c = Certificate.getBySerial(serial.toString(16));
            if (c.getOwner().getId() != subject.getId()) {
                return "Owner mismatch";
            }
            return PING_SUCCEDED;
        } catch (NoSuchAlgorithmException e) {
            // e.printStackTrace(); TODO log for user debugging?
            return "Security failed";
        } catch (SSLException e) {
            // e.printStackTrace(); TODO log for user debugging?
            return "Security failed";
        } catch (IOException e) {
            // e.printStackTrace(); TODO log for user debugging?
            return "Connection closed";
        }
    }
}
