package org.cacert.gigi;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRL;
import java.security.cert.CertificateException;
import java.util.Collection;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.cacert.gigi.natives.SetUID;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Launcher {
	public static void main(String[] args) throws Exception {
		Server s = new Server();
		// === SSL HTTP Configuration ===
		HttpConfiguration https_config = new HttpConfiguration();
		https_config.setSendServerVersion(false);
		https_config.setSendXPoweredBy(false);

		// for client-cert auth
		https_config.addCustomizer(new SecureRequestCustomizer());

		ServerConnector connector = new ServerConnector(s,
				new SslConnectionFactory(generateSSLContextFactory(),
						"http/1.1"), new HttpConnectionFactory(https_config));
		connector.setHost("127.0.0.1");
		connector.setPort(443);
		s.setConnectors(new Connector[]{connector});

		HandlerList hl = new HandlerList();
		hl.setHandlers(new Handler[]{generateStaticContext(),
				generateGigiContext()});
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

	private static ServletContextHandler generateGigiContext() {
		ServletContextHandler servlet = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		servlet.addServlet(new ServletHolder(new Gigi()), "/*");
		return servlet;
	}

	private static ContextHandler generateStaticContext() {
		ResourceHandler rh = new ResourceHandler();
		rh.setResourceBase("static");
		ContextHandler ch = new ContextHandler();
		ch.setHandler(rh);
		ch.setContextPath("/static");
		return ch;
	}

	private static SslContextFactory generateSSLContextFactory()
			throws NoSuchAlgorithmException, KeyStoreException, IOException,
			CertificateException, FileNotFoundException {
		TrustManagerFactory tmFactory = TrustManagerFactory.getInstance("PKIX");
		tmFactory.init((KeyStore) null);

		final TrustManager[] tm = tmFactory.getTrustManagers();

		SslContextFactory scf = new SslContextFactory() {
			@Override
			protected TrustManager[] getTrustManagers(KeyStore trustStore,
					Collection<? extends CRL> crls) throws Exception {
				return tm;
			}
		};
		scf.setWantClientAuth(true);
		KeyStore ks1 = KeyStore.getInstance("pkcs12");
		ks1.load(new FileInputStream("config/keystore.pkcs12"),
				"".toCharArray());
		scf.setKeyStore(ks1);
		scf.setProtocol("TLSv1");
		return scf;
	}
}
