package org.cacert.gigi;

import static org.cacert.gigi.Gigi.*;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.dbObjects.ObjectCache;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.RandomToken;
import org.cacert.gigi.util.ServerConstants;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarOutputStream;

public class DevelLauncher {

    public static void main(String[] args) throws Exception {
        Properties mainProps = new Properties();
        try (FileInputStream inStream = new FileInputStream("config/gigi.properties")) {
            mainProps.load(inStream);
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port")) {
                mainProps.setProperty("port", args[i + 1]);
            }
            i++;
        }
        killPreviousInstance(mainProps);

        ByteArrayOutputStream chunkConfig = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(chunkConfig);
        byte[] cacerts = Files.readAllBytes(Paths.get("config/cacerts.jks"));
        byte[] keystore = Files.readAllBytes(Paths.get("config/keystore.pkcs12"));

        DevelLauncher.writeGigiConfig(dos, "changeit".getBytes("UTF-8"), "changeit".getBytes("UTF-8"), mainProps, cacerts, keystore);
        dos.flush();
        InputStream oldin = System.in;
        System.setIn(new ByteArrayInputStream(chunkConfig.toByteArray()));
        new Launcher().boot();
        final String token = RandomToken.generateToken(32);
        addDevelPage(token);
        Desktop.getDesktop().browse(new URL("http://" + ServerConstants.getWwwHostNamePort() + "/ticket?token=" + token).toURI());
        System.setIn(oldin);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        System.out.println("Cacert-gigi system sucessfully started.");
        System.out.println("Press enter to shutdown.");
        br.readLine();
        System.exit(0);
    }

    private static void killPreviousInstance(Properties mainProps) {
        try {
            String targetPort = mainProps.getProperty("http.port");
            String targetHost = mainProps.getProperty("name.www");
            URL u = new URL("http://" + targetHost + ":" + targetPort + "/kill");
            u.openStream();
        } catch (IOException e) {
        }
    }

    public static void addDevelPage(String token) {
        try {
            Field instF = Gigi.class.getDeclaredField("instance");
            Field pageF = Gigi.class.getDeclaredField("pages");
            instF.setAccessible(true);
            pageF.setAccessible(true);
            Object gigi = instF.get(null);

            // Check if we got a proper map (as much as we can tell)
            Object pagesObj = pageF.get(gigi);
            @SuppressWarnings("unchecked")
            HashMap<String, Page> pages = pagesObj instanceof Map ? new HashMap<>((Map<String, Page>) pagesObj) : null;

            pages.put("/manage", new Page("Page-manager") {

                @Override
                public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    ObjectCache.clearAllCaches();
                    resp.getWriter().println("All caches cleared.");
                    System.out.println("Caches cleared.");

                }

                @Override
                public boolean needsLogin() {
                    return false;
                }

            });

            pages.put("/kill", new Page("Kill") {

                /**
                 * The contained call to {@link System#exit(int)} is mainly
                 * needed to kill this instance immediately if another
                 * {@link DevelLauncher} is booting up to free all ports This is
                 * required for fast development cycles.
                 * 
                 * @see #killPreviousInstance(Properties)
                 */
                @Override
                public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    System.exit(0);
                }

                @Override
                public boolean needsLogin() {
                    return false;
                }
            });

            if (token != null) {
                addTicketPage(pages, token);
            }

            pageF.set(gigi, Collections.unmodifiableMap(pages));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static void addTicketPage(HashMap<String, Page> pages, final String token) {
        pages.put("/ticket", new Page("ticket") {

            boolean used = false;

            @Override
            public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                if ( !used && token.equals(req.getParameter("token"))) {
                    HttpSession sess = req.getSession();
                    User user = User.getById(1);
                    sess.setAttribute(LOGGEDIN, true);
                    sess.setAttribute(Language.SESSION_ATTRIB_NAME, user.getPreferredLocale());
                    sess.setAttribute(USER, user);
                    req.getSession().setAttribute(LOGIN_METHOD, "Ticket");
                    resp.sendRedirect("/");
                }
            }

            @Override
            public boolean needsLogin() {
                return false;
            }
        });
    }

    public static void writeGigiConfig(OutputStream target, byte[] keystorepw, byte[] truststorepw, Properties mainprop, byte[] cacerts, byte[] keystore) throws IOException {
        TarOutputStream tos = new TarOutputStream(target);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mainprop.store(baos, "");

        putTarEntry(baos.toByteArray(), tos, "gigi.properties");
        putTarEntry(keystorepw, tos, "keystorepw");
        putTarEntry(truststorepw, tos, "truststorepw");
        putTarEntry(keystore, tos, "keystore.pkcs12");
        putTarEntry(cacerts, tos, "cacerts.jks");
        tos.close();

    }

    private static void putTarEntry(byte[] data, TarOutputStream tos, String name) throws IOException {
        TarHeader th = new TarHeader();
        th.name = new StringBuffer(name);
        th.size = data.length;
        tos.putNextEntry(new TarEntry(th));
        tos.write(data);
    }

}
