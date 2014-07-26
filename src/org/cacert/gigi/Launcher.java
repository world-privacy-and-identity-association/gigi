package org.cacert.gigi;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import org.cacert.gigi.api.GigiAPI;
import org.cacert.gigi.natives.SetUID;
import org.cacert.gigi.util.CipherInfo;
import org.cacert.gigi.util.ServerConstants;
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
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Launcher {

    public static void main(String[] args) throws Exception {
        GigiConfig conf = GigiConfig.parse(System.in);
        ServerConstants.init(conf.getMainProps());

        Server s = new Server();
        // === SSL HTTP Configuration ===
        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSendServerVersion(false);
        https_config.setSendXPoweredBy(false);

        // for client-cert auth
        https_config.addCustomizer(new SecureRequestCustomizer());

        ServerConnector connector = new ServerConnector(s, createConnectionFactory(conf), new HttpConnectionFactory(https_config));
        connector.setHost(conf.getMainProps().getProperty("host"));
        connector.setPort(Integer.parseInt(conf.getMainProps().getProperty("port")));
        s.setConnectors(new Connector[] {
            connector
        });

        HandlerList hl = new HandlerList();
        hl.setHandlers(new Handler[] {
                generateStaticContext(), generateGigiContexts(conf.getMainProps()), generateAPIContext()
        });
        s.setHandler(hl);
        s.start();
        if (connector.getPort() <= 1024 && !System.getProperty("os.name").toLowerCase().contains("win")) {
            SetUID uid = new SetUID();
            if ( !uid.setUid(65536 - 2, 65536 - 2).getSuccess()) {
                Log.getLogger(Launcher.class).warn("Couldn't set uid!");
            }
        }
    }

    private static SslConnectionFactory createConnectionFactory(GigiConfig conf) throws GeneralSecurityException, IOException {
        final SslContextFactory sslContextFactory = generateSSLContextFactory(conf, "www");
        final SslContextFactory secureContextFactory = generateSSLContextFactory(conf, "secure");
        secureContextFactory.setWantClientAuth(true);
        secureContextFactory.setNeedClientAuth(false);
        final SslContextFactory staticContextFactory = generateSSLContextFactory(conf, "static");
        final SslContextFactory apiContextFactory = generateSSLContextFactory(conf, "api");
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

    private static Handler generateGigiContexts(Properties conf) {
        ServletHolder webAppServlet = new ServletHolder(new Gigi(conf));

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
        final ResourceHandler rh = new ResourceHandler();
        rh.setResourceBase("static/www");

        HandlerWrapper hw = new PolicyRedirector();
        hw.setHandler(rh);

        ServletContextHandler servlet = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servlet.setInitParameter(SessionManager.__SessionCookieProperty, "CACert-Session");
        servlet.addServlet(webAppServlet, "/*");
        ErrorPageErrorHandler epeh = new ErrorPageErrorHandler();
        epeh.addErrorPage(404, "/error");
        servlet.setErrorHandler(epeh);

        HandlerList hl = new HandlerList();
        hl.setHandlers(new Handler[] {
                hw, servlet
        });

        ContextHandler ch = new ContextHandler();
        ch.setHandler(hl);
        return ch;
    }

    private static Handler generateStaticContext() {
        final ResourceHandler rh = new ResourceHandler();
        rh.setResourceBase("static/static");

        ContextHandler ch = new ContextHandler();
        ch.setHandler(rh);
        ch.setVirtualHosts(new String[] {
            ServerConstants.getStaticHostName()
        });

        return ch;
    }

    private static Handler generateAPIContext() {
        ServletContextHandler sch = new ServletContextHandler();

        sch.addVirtualHosts(new String[] {
            ServerConstants.getApiHostName()
        });
        sch.addServlet(new ServletHolder(new GigiAPI()), "/*");
        return sch;
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
        scf.setTrustStore(conf.getTrustStore());
        KeyStore privateStore = conf.getPrivateStore();
        scf.setKeyStorePassword(conf.getPrivateStorePw());
        scf.setKeyStore(privateStore);
        scf.setCertAlias(alias);
        return scf;
    }
}
