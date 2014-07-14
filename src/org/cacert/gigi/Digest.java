package org.cacert.gigi;

public enum Digest {
	SHA256("Currently recommended, because the other algorithms"
		+ " might break on some older versions of the GnuTLS library"
		+ " (older than 3.x) still shipped in Debian for example."), SHA384(null), SHA512(
		"Highest protection against hash collision attacks of the algorithms offered here.");
	final String exp;

	private Digest(String explanation) {
		exp = explanation;
	}

	public String getExp() {
		return exp;
	}

	public static Digest getDefault() {
		return SHA256;
	}
}
