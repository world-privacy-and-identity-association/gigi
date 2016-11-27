package org.cacert.gigi.ping;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
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
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.cacert.gigi.dbObjects.CACertificate;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;

import sun.security.x509.AVA;
import sun.security.x509.X500Name;

public class SSLPinger extends DomainPinger {

    public static final String[] TYPES = new String[] {
            "xmpp", "server-xmpp", "smtp", "imap"
    };

    private KeyStore truststore;

    public SSLPinger(KeyStore truststore) {
        this.truststore = truststore;
    }

    @Override
    public void ping(Domain domain, String configuration, CertificateOwner u, int confId) {
        try (SocketChannel sch = SocketChannel.open()) {
            sch.socket().setSoTimeout(5000);
            String[] parts = configuration.split(":", 4);
            sch.socket().connect(new InetSocketAddress(domain.getSuffix(), Integer.parseInt(parts[2])), 5000);
            if (parts.length == 4) {
                switch (parts[3]) {
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
            String key = parts[0];
            String value = parts[1];
            String res = test(sch, domain.getSuffix(), u, value);
            enterPingResult(confId, res, res, null);
            return;
        } catch (IOException e) {
            enterPingResult(confId, "error", "connection Failed", null);
            return;
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

    private String test(SocketChannel sch, String domain, CertificateOwner subject, String tok) {
        System.out.println("SSL- connecting");

        try {
            sch.socket().setSoTimeout(5000);
            SSLContext sc = SSLContext.getInstance("SSL");
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                tmf.init(truststore);
                sc.init(null, new TrustManager[] {
                        new X509TrustManager() {

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                                java.security.cert.X509Certificate c = chain[0];
                                if (c.getExtendedKeyUsage() != null && !c.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.1")) {
                                    throw new java.security.cert.CertificateException("Illegal EKU");
                                }
                            }

                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {}
                        }
                }, new SecureRandom());
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
            System.out.println("SSL- connected");
            X509Certificate[] peerCertificateChain = se.getSession().getPeerCertificateChain();
            X509Certificate first = peerCertificateChain[0];
            if (first.getIssuerDN().equals(first.getSubjectDN())) {
                first.verify(first.getPublicKey());
                X500Name p = (X500Name) first.getSubjectDN();
                X500Name n = new X500Name(p.getEncoded());
                for (AVA i : n.allAvas()) {
                    if (i.getObjectIdentifier().equals((Object) X500Name.orgUnitName_oid)) {
                        String toke = i.getDerValue().getAsString();
                        if (tok.equals(toke)) {
                            return PING_SUCCEDED;
                        } else {
                            return "Self-signed certificate is wrong";
                        }
                    }
                }
            }

            BigInteger serial = first.getSerialNumber();
            Certificate c = Certificate.getBySerial(serial.toString(16));
            if (c == null) {
                return "Certificate not found: Serial " + serial.toString(16) + " missing.";
            }
            CACertificate p = c.getParent();
            if ( !first.getIssuerDN().equals(p.getCertificate().getSubjectDN())) {
                return "Broken certificate supplied";
            }
            first.verify(p.getCertificate().getPublicKey());
            if (c.getOwner().getId() != subject.getId()) {
                return "Owner mismatch";
            }
            return PING_SUCCEDED;
        } catch (GeneralSecurityException e) {
            // e.printStackTrace();
            return "Security failed";
        } catch (SSLException e) {
            // e.printStackTrace(); TODO log for user debugging?
            return "Security failed";
        } catch (IOException e) {
            // e.printStackTrace(); TODO log for user debugging?
            return "Connection closed";
        } catch (CertificateException e) {
            // e.printStackTrace();
            return "Security failed";
        }
    }
}
