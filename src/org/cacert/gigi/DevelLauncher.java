package org.cacert.gigi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class DevelLauncher {
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
		byte[] keystore = Files.readAllBytes(Paths
				.get("config/keystore.pkcs12"));

		DevelLauncher.writeGigiConfig(dos, new byte[]{}, "changeit".getBytes(),
				mainProps, cacerts, keystore);
		dos.flush();
		InputStream oldin = System.in;
		System.setIn(new ByteArrayInputStream(chunkConfig.toByteArray()));
		Launcher.main(args);
		System.setIn(oldin);
	}
	public static void writeGigiConfig(DataOutputStream target,
			byte[] keystorepw, byte[] truststorepw, Properties mainprop,
			byte[] cacerts, byte[] keystore) throws IOException {
		writeChunk(target, GigiConfig.GIGI_CONFIG_VERSION.getBytes());
		writeChunk(target, keystorepw);
		writeChunk(target, truststorepw);
		ByteArrayOutputStream props = new ByteArrayOutputStream();
		mainprop.store(props, "");
		writeChunk(target, props.toByteArray());
		writeChunk(target, cacerts);
		writeChunk(target, keystore);

	}
	public static void writeChunk(DataOutputStream dos, byte[] chunk)
			throws IOException {
		dos.writeInt(chunk.length);
		dos.write(chunk);
	}
	public static void launch(Properties props, File cacerts, File keystore)
			throws IOException {
		ByteArrayOutputStream config = new ByteArrayOutputStream();
		props.store(config, "");
	}
}
