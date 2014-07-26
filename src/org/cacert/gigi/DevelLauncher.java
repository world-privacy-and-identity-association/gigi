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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

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

        ByteArrayOutputStream chunkConfig = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(chunkConfig);
        byte[] cacerts = Files.readAllBytes(Paths.get("config/cacerts.jks"));
        byte[] keystore = Files.readAllBytes(Paths.get("config/keystore.pkcs12"));

        DevelLauncher.writeGigiConfig(dos, "changeit".getBytes(), "changeit".getBytes(), mainProps, cacerts, keystore);
        dos.flush();
        InputStream oldin = System.in;
        System.setIn(new ByteArrayInputStream(chunkConfig.toByteArray()));
        Launcher.main(args);
        System.setIn(oldin);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Cacert-gigi system sucessfully started.");
        System.out.println("Press enter to shutdown.");
        br.readLine();
        System.exit(0);
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
