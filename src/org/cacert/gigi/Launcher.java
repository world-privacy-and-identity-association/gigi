package org.cacert.gigi;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Properties;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import org.cacert.gigi.natives.SetUID;
import org.cacert.gigi.util.CipherInfo;
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
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Launcher {
	public static void main(String[] args) throws Exception {
		GigiConfig conf = GigiConfig.parse(System.in);

		Server s = new Server();
		// === SSL HTTP Configuration ===
		HttpConfiguration https_config = new HttpConfiguration();
		https_config.setSendServerVersion(false);
		https_config.setSendXPoweredBy(false);

		// for client-cert auth
		https_config.addCustomizer(new SecureRequestCustomizer());

		ServerConnector connector = new ServerConnector(s,
				new SslConnectionFactory(generateSSLContextFactory(conf),
						"http/1.1"), new HttpConnectionFactory(https_config));
		connector.setHost(conf.getMainProps().getProperty("host"));
		connector.setPort(Integer.parseInt(conf.getMainProps().getProperty(
				"port")));
		s.setConnectors(new Connector[]{connector});

		HandlerList hl = new HandlerList();
		hl.setHandlers(new Handler[]{generateStaticContext(),
				generateGigiContext(conf.getMainProps())});
		s.setHandler(hl);
		s.start();
		if (connector.getPort() <= 1024
				&& !System.getProperty("os.name").toLowerCase().contains("win")) {
			SetUID uid = new SetUID();
			if (!uid.setUid(-2, -2).getSuccess()) {
				Log.getLogger(Launcher.class).warn("Couldn't set uid!");
			}
		}
	}

	private static ServletContextHandler generateGigiContext(Properties conf) {
		ServletContextHandler servlet = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		servlet.setInitParameter(SessionManager.__SessionCookieProperty,
				"CACert-Session");
		servlet.addServlet(new ServletHolder(new Gigi(conf)), "/*");
		return servlet;
	}

	private static Handler generateStaticContext() {
		final ResourceHandler rh = new ResourceHandler();
		rh.setResourceBase("static");
		HandlerWrapper hw = new PolicyRedirector();
		hw.setHandler(rh);

		ContextHandler ch = new ContextHandler();
		ch.setContextPath("/static");
		ch.setHandler(hw);

		return ch;
	}

	private static SslContextFactory generateSSLContextFactory(GigiConfig conf)
			throws GeneralSecurityException, IOException {
		TrustManagerFactory tmFactory = TrustManagerFactory.getInstance("PKIX");
		tmFactory.init((KeyStore) null);

		SslContextFactory scf = new SslContextFactory() {

			String[] ciphers = null;

			@Override
			public void customize(SSLEngine sslEngine) {
				super.customize(sslEngine);

				SSLParameters ssl = sslEngine.getSSLParameters();
				ssl.setUseCipherSuitesOrder(true);
				if (ciphers == null) {
					ciphers = CipherInfo.filter(sslEngine
							.getSupportedCipherSuites());
				}

				ssl.setCipherSuites(ciphers);
				sslEngine.setSSLParameters(ssl);

			}

		};
		scf.setRenegotiationAllowed(false);
		scf.setWantClientAuth(true);

		scf.setProtocol("TLS");
		scf.setTrustStore(conf.getTrustStore());
		scf.setKeyStore(conf.getPrivateStore());
		return scf;
	}
}
