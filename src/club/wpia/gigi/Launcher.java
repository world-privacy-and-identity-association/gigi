package club.wpia.gigi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import club.wpia.gigi.api.GigiAPI;
import club.wpia.gigi.email.EmailProvider;
import club.wpia.gigi.natives.SetUID;
import club.wpia.gigi.ocsp.OCSPResponder;
import club.wpia.gigi.util.PEM;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

public class Launcher {

    class ExtendedForwarded implements Customizer {

        @Override
        public void customize(Connector connector, HttpConfiguration config, Request request) {
            HttpFields httpFields = request.getHttpFields();

            String ip = httpFields.getStringField("X-Real-IP");
            String proto = httpFields.getStringField("X-Real-Proto");
            String cert = httpFields.getStringField("X-Client-Cert");
            request.setSecure("https".equals(proto));
            request.setScheme(proto);
            if ( !"https".equals(proto)) {
                cert = null;

            }
            if (cert != null) {
                X509Certificate[] certs = new X509Certificate[1];
                try {
                    certs[0] = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(PEM.decode("CERTIFICATE", cert)));
                    request.setAttribute("javax.servlet.request.X509Certificate", certs);
                } catch (CertificateException e) {
                    e.printStackTrace();
                }
            }
            if (ip != null) {
                String[] parts = ip.split(":");
                if (parts.length == 2) {
                    request.setRemoteAddr(InetSocketAddress.createUnresolved(parts[0], Integer.parseInt(parts[1])));
                }
            }

        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.tls.ephemeralDHKeySize", "4096");
        InputStream in;
        if (args.length >= 1) {
            in = new FileInputStream(new File(args[0]));
        } else {
            in = System.in;
        }
        new Launcher().boot(in);
    }

    Server s;

    GigiConfig conf;

    private boolean isSystemPort(int port) {
        return 1 <= port && port <= 1024;
    }

    public synchronized void boot(InputStream in) throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        HttpURLConnection.setFollowRedirects(false);

        conf = GigiConfig.parse(in);
        ServerConstants.init(conf.getMainProps());
        initEmails(conf);

        s = new Server();

        initConnectors();
        initHandlers();

        s.start();
        if ((isSystemPort(ServerConstants.getSecurePort()) || isSystemPort(ServerConstants.getPort())) && !System.getProperty("os.name").toLowerCase().contains("win")) {
            String uid_s = conf.getMainProps().getProperty("gigi.uid", Integer.toString(65536 - 2));
            String gid_s = conf.getMainProps().getProperty("gigi.gid", Integer.toString(65536 - 2));
            try {
                int uid = Integer.parseInt(uid_s);
                int gid = Integer.parseInt(gid_s);
                if (uid == -1 && gid == -1) {
                    // skip setuid step
                } else if (uid > 0 && gid > 0 && uid < 65536 && gid < 65536) {
                    SetUID.Status status = new SetUID().setUid(uid, gid);
                    if ( !status.getSuccess()) {
                        Log.getLogger(Launcher.class).warn(status.getMessage());
                    }
                } else {
                    Log.getLogger(Launcher.class).warn("Invalid uid or gid (must satisfy 0 < id < 65536)");
                }
            } catch (NumberFormatException e) {
                Log.getLogger(Launcher.class).warn("Invalid gigi.uid or gigi.gid", e);
            }
        }
    }

    private HttpConfiguration createHttpConfiguration() {
        // SSL HTTP Configuration
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSendServerVersion(false);
        httpsConfig.setSendXPoweredBy(false);
        return httpsConfig;
    }

    private void initConnectors() throws GeneralSecurityException, IOException {
        HttpConfiguration httpConfig = createHttpConfiguration();
        if (conf.getMainProps().getProperty("proxy", "false").equals("true")) {
            httpConfig.addCustomizer(new ExtendedForwarded());
            s.setConnectors(new Connector[] {
                    ConnectorsLauncher.createConnector(conf, s, httpConfig, false)
            });
        } else {
            HttpConfiguration httpsConfig = createHttpConfiguration();
            // for client-cert auth
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            s.setConnectors(new Connector[] {
                    ConnectorsLauncher.createConnector(conf, s, httpsConfig, true), ConnectorsLauncher.createConnector(conf, s, httpConfig, false)
            });
        }
    }

    private void initEmails(GigiConfig conf) throws GeneralSecurityException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore privateStore = conf.getPrivateStore();
        Certificate mail = null;
        Key k = null;
        if (privateStore != null && privateStore.containsAlias("mail")) {
            mail = privateStore.getCertificate("mail");
            k = privateStore.getKey("mail", conf.getPrivateStorePw().toCharArray());
        }
        EmailProvider.initSystem(conf.getMainProps(), mail, k);
    }

    private static class ConnectorsLauncher {

        private ConnectorsLauncher() {}

        protected static ServerConnector createConnector(GigiConfig conf, Server s, HttpConfiguration httpConfig, boolean doHttps) throws GeneralSecurityException, IOException {
            ServerConnector connector;
            int port;
            if (doHttps) {
                connector = new ServerConnector(s, createConnectionFactory(conf), new HttpConnectionFactory(httpConfig));
                port = ServerConstants.getSecurePort();
            } else {
                connector = new ServerConnector(s, new HttpConnectionFactory(httpConfig));
                port = ServerConstants.getPort();
            }
            if (port == -1) {
                connector.setInheritChannel(true);
            } else {
                connector.setHost(conf.getMainProps().getProperty("host"));
                connector.setPort(port);
            }
            connector.setAcceptQueueSize(100);
            return connector;
        }

        private static SslConnectionFactory createConnectionFactory(GigiConfig conf) throws GeneralSecurityException, IOException {
            final SslContextFactory sslContextFactory = generateSSLContextFactory(conf, "www");
            final SslContextFactory secureContextFactory = generateSSLContextFactory(conf, "secure");
            secureContextFactory.setWantClientAuth(true);
            secureContextFactory.setNeedClientAuth(true);
            final SslContextFactory staticContextFactory = generateSSLContextFactory(conf, "static");
            final SslContextFactory apiContextFactory = generateSSLContextFactory(conf, "api");
            apiContextFactory.setWantClientAuth(true);
            try {
                secureContextFactory.start();
                staticContextFactory.start();
                apiContextFactory.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()) {

                @Override
                public boolean shouldRestartSSL() {
                    return true;
                }

                @Override
                public SSLEngine restartSSL(SSLSession sslSession) {
                    SSLEngine e2 = null;
                    if (sslSession instanceof ExtendedSSLSession) {
                        ExtendedSSLSession es = (ExtendedSSLSession) sslSession;
                        List<SNIServerName> names = es.getRequestedServerNames();
                        for (SNIServerName sniServerName : names) {
                            if (sniServerName instanceof SNIHostName) {
                                SNIHostName host = (SNIHostName) sniServerName;
                                String hostname = host.getAsciiName();
                                if (hostname.equals(ServerConstants.getHostName(Host.WWW))) {
                                    e2 = sslContextFactory.newSSLEngine();
                                } else if (hostname.equals(ServerConstants.getHostName(Host.STATIC))) {
                                    e2 = staticContextFactory.newSSLEngine();
                                } else if (hostname.equals(ServerConstants.getHostName(Host.SECURE))) {
                                    e2 = secureContextFactory.newSSLEngine();
                                } else if (hostname.equals(ServerConstants.getHostName(Host.API))) {
                                    e2 = apiContextFactory.newSSLEngine();
                                }
                                break;
                            }
                        }
                    }
                    if (e2 == null) {
                        e2 = sslContextFactory.newSSLEngine(sslSession.getPeerHost(), sslSession.getPeerPort());
                    }
                    e2.setUseClientMode(false);
                    return e2;
                }
            };
        }

        private static SslContextFactory generateSSLContextFactory(GigiConfig conf, String alias) throws GeneralSecurityException, IOException {
            SslContextFactory scf = new SslContextFactory() {

                @Override
                public void customize(SSLEngine sslEngine) {
                    super.customize(sslEngine);

                    SSLParameters ssl = sslEngine.getSSLParameters();
                    ssl.setUseCipherSuitesOrder(true);
                    sslEngine.setSSLParameters(ssl);

                }

            };
            scf.setRenegotiationAllowed(false);

            scf.setProtocol("TLS");
            scf.setIncludeProtocols("TLSv1", "TLSv1.1", "TLSv1.2");
            scf.setTrustStore(conf.getTrustStore());
            KeyStore privateStore = conf.getPrivateStore();
            scf.setKeyStorePassword(conf.getPrivateStorePw());
            scf.setKeyStore(privateStore);
            scf.setCertAlias(alias);
            return scf;
        }
    }

    private void initHandlers() throws GeneralSecurityException, IOException {
        HandlerList hl = new HandlerList();
        hl.setHandlers(new Handler[] {
                ContextLauncher.generateStaticContext(), ContextLauncher.generateGigiContexts(conf.getMainProps(), conf.getTrustStore()), ContextLauncher.generateAPIContext(), ContextLauncher.generateOCSPContext()
        });
        s.setHandler(hl);
    }

    private static class ContextLauncher {

        private ContextLauncher() {}

        protected static Handler generateGigiContexts(Properties conf, KeyStore trust) {
            ServletHolder webAppServlet = new ServletHolder(new Gigi(conf, trust));

            ContextHandler ch = generateGigiServletContext(webAppServlet);
            ch.setVirtualHosts(new String[] {
                    ServerConstants.getHostName(Host.WWW)
            });
            ContextHandler chSecure = generateGigiServletContext(webAppServlet);
            chSecure.setVirtualHosts(new String[] {
                    ServerConstants.getHostName(Host.SECURE)
            });

            HandlerList hl = new HandlerList();
            hl.setHandlers(new Handler[] {
                    ch, chSecure
            });
            return hl;
        }

        private static ContextHandler generateGigiServletContext(ServletHolder webAppServlet) {
            final ResourceHandler rh = generateResourceHandler();
            rh.setResourceBase("static/www");

            HandlerWrapper hw = new HandlerWrapper();
            hw.setHandler(rh);

            ServletContextHandler servlet = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servlet.setInitParameter(SessionManager.__SessionCookieProperty, ServerConstants.getAppName() + "-Session");
            servlet.addServlet(webAppServlet, "/*");
            ErrorPageErrorHandler epeh = new ErrorPageErrorHandler();
            epeh.addErrorPage(404, "/error");
            epeh.addErrorPage(403, "/denied");
            servlet.setErrorHandler(epeh);

            HandlerList hl = new HandlerList();
            hl.setHandlers(new Handler[] {
                    hw, servlet
            });

            ContextHandler ch = new ContextHandler();
            ch.setHandler(hl);
            return ch;
        }

        protected static Handler generateStaticContext() {
            final ResourceHandler rh = generateResourceHandler();
            rh.setResourceBase("static/static");

            ContextHandler ch = new ContextHandler();
            ch.setHandler(rh);
            ch.setVirtualHosts(new String[] {
                    ServerConstants.getHostName(Host.STATIC)
            });

            return ch;
        }

        private static ResourceHandler generateResourceHandler() {
            ResourceHandler rh = new ResourceHandler() {

                @Override
                protected void doResponseHeaders(HttpServletResponse response, Resource resource, String mimeType) {
                    super.doResponseHeaders(response, resource, mimeType);
                    response.setDateHeader(HttpHeader.EXPIRES.asString(), System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7);
                }
            };
            rh.setEtags(true);
            return rh;
        }

        protected static Handler generateAPIContext() {
            ServletContextHandler sch = new ServletContextHandler();

            sch.addVirtualHosts(new String[] {
                    ServerConstants.getHostName(Host.API)
            });
            sch.addServlet(new ServletHolder(new GigiAPI()), "/*");
            return sch;
        }

        protected static Handler generateOCSPContext() {
            ServletContextHandler sch = new ServletContextHandler();

            sch.addVirtualHosts(new String[] {
                    ServerConstants.getHostName(Host.OCSP_RESPONDER)
            });
            sch.addServlet(new ServletHolder(new OCSPResponder()), "/*");
            return sch;
        }
    }

}
