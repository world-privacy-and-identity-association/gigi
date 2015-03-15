package org.cacert.gigi;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
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

import org.cacert.gigi.api.GigiAPI;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.natives.SetUID;
import org.cacert.gigi.util.CipherInfo;
import org.cacert.gigi.util.ServerConstants;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
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

public class Launcher {

    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.tls.ephemeralDHKeySize", "4096");
        new Launcher().boot();
    }

    Server s;

    GigiConfig conf;

    public synchronized void boot() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        conf = GigiConfig.parse(System.in);
        ServerConstants.init(conf.getMainProps());
        initEmails(conf);

        s = new Server();

        initConnectors();
        initHandlers();

        s.start();
        if ((ServerConstants.getSecurePort() <= 1024 || ServerConstants.getPort() <= 1024) && !System.getProperty("os.name").toLowerCase().contains("win")) {
            SetUID uid = new SetUID();
            if ( !uid.setUid(65536 - 2, 65536 - 2).getSuccess()) {
                Log.getLogger(Launcher.class).warn("Couldn't set uid!");
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
        HttpConfiguration httpsConfig = createHttpConfiguration();
        // for client-cert auth
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        HttpConfiguration httpConfig = createHttpConfiguration();
        s.setConnectors(new Connector[] {
                ConnectorsLauncher.createConnector(conf, s, httpsConfig, true), ConnectorsLauncher.createConnector(conf, s, httpConfig, false)
        });
    }

    private void initEmails(GigiConfig conf) throws GeneralSecurityException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore privateStore = conf.getPrivateStore();
        Certificate mail = privateStore.getCertificate("mail");
        Key k = privateStore.getKey("mail", conf.getPrivateStorePw().toCharArray());
        EmailProvider.initSystem(conf.getMainProps(), mail, k);
    }

    private static class ConnectorsLauncher {

        private ConnectorsLauncher() {}

        protected static ServerConnector createConnector(GigiConfig conf, Server s, HttpConfiguration httpConfig, boolean doHttps) throws GeneralSecurityException, IOException {
            ServerConnector connector;
            if (doHttps) {
                connector = new ServerConnector(s, createConnectionFactory(conf), new HttpConnectionFactory(httpConfig));
            } else {
                connector = new ServerConnector(s, new HttpConnectionFactory(httpConfig));
            }
            connector.setHost(conf.getMainProps().getProperty("host"));
            if (doHttps) {
                connector.setPort(ServerConstants.getSecurePort());
            } else {
                connector.setPort(ServerConstants.getPort());
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
                                if (hostname.equals(ServerConstants.getWwwHostName())) {
                                    e2 = sslContextFactory.newSSLEngine();
                                } else if (hostname.equals(ServerConstants.getStaticHostName())) {
                                    e2 = staticContextFactory.newSSLEngine();
                                } else if (hostname.equals(ServerConstants.getSecureHostName())) {
                                    e2 = secureContextFactory.newSSLEngine();
                                } else if (hostname.equals(ServerConstants.getApiHostName())) {
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

                String[] ciphers = null;

                @Override
                public void customize(SSLEngine sslEngine) {
                    super.customize(sslEngine);

                    SSLParameters ssl = sslEngine.getSSLParameters();
                    ssl.setUseCipherSuitesOrder(true);
                    if (ciphers == null) {
                        ciphers = CipherInfo.filter(sslEngine.getSupportedCipherSuites());
                    }

                    ssl.setCipherSuites(ciphers);
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
                ContextLauncher.generateStaticContext(), ContextLauncher.generateGigiContexts(conf.getMainProps(), conf.getTrustStore()), ContextLauncher.generateAPIContext()
        });
        s.setHandler(hl);
    }

    private static class ContextLauncher {

        private ContextLauncher() {}

        protected static Handler generateGigiContexts(Properties conf, KeyStore trust) {
            ServletHolder webAppServlet = new ServletHolder(new Gigi(conf, trust));

            ContextHandler ch = generateGigiServletContext(webAppServlet);
            ch.setVirtualHosts(new String[] {
                ServerConstants.getWwwHostName()
            });
            ContextHandler chSecure = generateGigiServletContext(webAppServlet);
            chSecure.setVirtualHosts(new String[] {
                ServerConstants.getSecureHostName()
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

            HandlerWrapper hw = new PolicyRedirector();
            hw.setHandler(rh);

            ServletContextHandler servlet = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servlet.setInitParameter(SessionManager.__SessionCookieProperty, "CACert-Session");
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
                ServerConstants.getStaticHostName()
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
                ServerConstants.getApiHostName()
            });
            sch.addServlet(new ServletHolder(new GigiAPI()), "/*");
            return sch;
        }

    }

}
