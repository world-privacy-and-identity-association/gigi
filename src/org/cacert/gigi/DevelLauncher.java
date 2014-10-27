package org.cacert.gigi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.ObjectCache;
import org.cacert.gigi.pages.Page;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarOutputStream;

public class DevelLauncher {

    public static final boolean DEVEL = true;

    public static void main(String[] args) throws Exception {
        Properties mainProps = new Properties();
        mainProps.load(new FileInputStream("config/gigi.properties"));
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port")) {
                mainProps.setProperty("port", args[i + 1]);
            }
            i++;
        }
        try {
            String targetPort = mainProps.getProperty("http.port");
            String targetHost = mainProps.getProperty("name.www");
            URL u = new URL("http://" + targetHost + ":" + targetPort + "/kill");
            u.openStream();
        } catch (IOException e) {
        }

        ByteArrayOutputStream chunkConfig = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(chunkConfig);
        byte[] cacerts = Files.readAllBytes(Paths.get("config/cacerts.jks"));
        byte[] keystore = Files.readAllBytes(Paths.get("config/keystore.pkcs12"));

        DevelLauncher.writeGigiConfig(dos, "changeit".getBytes(), "changeit".getBytes(), mainProps, cacerts, keystore);
        dos.flush();
        InputStream oldin = System.in;
        System.setIn(new ByteArrayInputStream(chunkConfig.toByteArray()));
        Launcher.main(args);
        addDevelPage();
        System.setIn(oldin);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Cacert-gigi system sucessfully started.");
        System.out.println("Press enter to shutdown.");
        br.readLine();
        System.exit(0);
    }

    private static void addDevelPage() {
        try {
            Field instF = Gigi.class.getDeclaredField("instance");
            Field pageF = Gigi.class.getDeclaredField("pages");
            instF.setAccessible(true);
            pageF.setAccessible(true);
            Object gigi = instF.get(null);
            HashMap<String, Page> pages = (HashMap<String, Page>) pageF.get(gigi);
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

                @Override
                public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                    System.exit(0);
                }

                @Override
                public boolean needsLogin() {
                    return false;
                }
            });
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
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

    public static void writeChunk(DataOutputStream dos, byte[] chunk) throws IOException {
        dos.writeInt(chunk.length);
        dos.write(chunk);
    }

    public static void launch(Properties props, File cacerts, File keystore) throws IOException {
        ByteArrayOutputStream config = new ByteArrayOutputStream();
        props.store(config, "");
    }
}
