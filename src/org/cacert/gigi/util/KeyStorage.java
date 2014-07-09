package org.cacert.gigi.util;

import java.io.File;

public class KeyStorage {
	private static final File csr = new File("keys/csr");
	private static final File crt = new File("keys/crt");

	public static File locateCrt(int id) {
		File parent = new File(crt, (id / 1000) + "");
		parent.mkdirs();
		return new File(parent, id + ".crt");
	}

	public static File locateCsr(int id) {
		File parent = new File(csr, (id / 1000) + "");
		parent.mkdirs();
		return new File(parent, id + ".csr");
	}
}
