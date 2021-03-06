package club.wpia.gigi;

import static club.wpia.gigi.Gigi.*;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarOutputStream;

import club.wpia.gigi.dbObjects.ObjectCache;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.pages.account.certs.CertificateRequest;
import club.wpia.gigi.pages.main.RegisterPage;
import club.wpia.gigi.util.AuthorizationContext;
import club.wpia.gigi.util.ServerConstants;
import club.wpia.gigi.util.ServerConstants.Host;

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
        byte[] keystore = null;
        Path p = Paths.get("config/keystore.pkcs12");
        if (p.toFile().exists()) {
            keystore = Files.readAllBytes(p);
        } else {
            mainProps.setProperty("proxy", "true");
        }

        DevelLauncher.writeGigiConfig(dos, "changeit".getBytes("UTF-8"), "changeit".getBytes("UTF-8"), mainProps, cacerts, keystore);
        dos.flush();
        new Launcher().boot(new ByteArrayInputStream(chunkConfig.toByteArray()));
        addDevelPage(true);
        new Thread("ticket awaiter") {

            @Override
            public void run() {
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    if ( !ticketUsed) {
                        Desktop.getDesktop().browse(new URL("http://" + ServerConstants.getHostNamePort(Host.WWW) + "/ticketWait").toURI());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        System.out.println("Gigi system sucessfully started.");
        System.out.println("Press enter to shutdown.");
        br.readLine();
        System.exit(0);
    }

    private static void killPreviousInstance(Properties mainProps) {
        try {
            String targetPort = mainProps.getProperty("http.port");
            String targetHost = mainProps.getProperty("name.www", "www." + mainProps.getProperty("name.suffix"));
            URL u = new URL("http://" + targetHost + ":" + targetPort + "/kill");
            u.openStream();
        } catch (IOException e) {
        }
    }

    public static void addDevelPage(boolean withToken) {
        try {
            Field instF = Gigi.class.getDeclaredField("instance");
            Field pageF = Gigi.class.getDeclaredField("pages");
            instF.setAccessible(true);
            pageF.setAccessible(true);
            Object gigi = instF.get(null);

            // Check if we got a proper map (as much as we can tell)
            Object pagesObj = pageF.get(gigi);
            if ( !(pagesObj instanceof Map)) {
                throw new Error("Invalid state when initializing page structure");
            }

            @SuppressWarnings("unchecked")
            HashMap<String, Page> pages = new HashMap<>((Map<String, Page>) pagesObj);

            pages.put("/manage", new Page("Page-manager") {

                @Override
                public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    ObjectCache.clearAllCaches();
                    RegisterPage.RATE_LIMIT.bypass();
                    LoginPage.RATE_LIMIT.bypass();
                    CertificateRequest.RATE_LIMIT.bypass();
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

            if (withToken) {
                addTicketPage(pages);
            }

            pageF.set(gigi, Collections.unmodifiableMap(pages));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    static boolean ticketUsed = false;

    private static void addTicketPage(HashMap<String, Page> pages) {
        pages.put("/ticketWait", new Page("ticket") {

            private final Template t = new Template(DevelLauncher.class.getResource("DevelTicketWait.templ"));

            @Override
            public boolean needsLogin() {
                return false;
            }

            @Override
            public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setHeader("content-security-policy", "");
                t.output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
            }

        });
        pages.put("/ticket", new Page("ticket") {

            @Override
            public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                // TODO Auto-generated method stub
                if ( !ticketUsed) {
                    HttpSession sess = req.getSession();
                    User user = User.getById(1);
                    if (user == null) {
                        resp.getWriter().println("ticket consumed but no user available for that action");
                        ticketUsed = true;
                        return true;
                    }
                    sess.setAttribute(LOGGEDIN, true);
                    sess.setAttribute(Language.SESSION_ATTRIB_NAME, user.getPreferredLocale());
                    // ac.isStronglyAuthenticated() set to true to bypass
                    // certificate login for testing
                    sess.setAttribute(AUTH_CONTEXT, new AuthorizationContext(user, user, true));
                    req.getSession().setAttribute(LOGIN_METHOD, new TranslateCommand("Ticket"));
                    resp.getWriter().println("ticket consumed");
                    ticketUsed = true;
                }
                return true;
            }

            @Override
            public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {}

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
        if (data == null) {
            return;
        }
        TarHeader th = new TarHeader();
        th.name = new StringBuffer(name);
        th.size = data.length;
        tos.putNextEntry(new TarEntry(th));
        tos.write(data);
    }

}
