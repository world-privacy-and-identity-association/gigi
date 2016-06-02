package org.cacert.gigi.localisation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

public class TaintSource {
	private String pack, cls, meth;
	private int tgt;
	private TaintSource maskOnly;

	public TaintSource(String pack, String cls, String meth, int tgt) {
		this(pack, cls, meth, tgt, null);
	}
	public TaintSource(String pack, String cls, String meth, int tgt,
			TaintSource maskOnly) {
		this.pack = pack;
		this.cls = cls;
		this.meth = meth;
		this.tgt = tgt;
		this.maskOnly = maskOnly;

	}
	public TaintSource(MethodBinding mb) {
		pack = new String(mb.declaringClass.qualifiedPackageName());
		cls = new String(mb.declaringClass.qualifiedSourceName());
		meth = new String(mb.readableName());
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cls == null) ? 0 : cls.hashCode());
		result = prime * result + ((meth == null) ? 0 : meth.hashCode());
		result = prime * result + ((pack == null) ? 0 : pack.hashCode());
		return result;
	}
	@Override
	public String toString() {
		StringBuffer res = new StringBuffer();
		res.append("new TaintSource(");
		res.append("\"" + pack + "\",");
		res.append("\"" + cls + "\",");
		res.append("\"" + meth + "\",0);");
		return res.toString();
	}
	public String toConfLine() {
		return pack + " " + cls + "." + meth + "," + tgt
				+ (maskOnly == null ? "" : "=>" + maskOnly.toConfLine());
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TaintSource other = (TaintSource) obj;
		if (cls == null) {
			if (other.cls != null) {
				return false;
			}
		} else if (!cls.equals(other.cls)) {
			return false;
		}
		if (pack == null) {
			if (other.pack != null) {
				return false;
			}
		} else if (!pack.equals(other.pack)) {
			return false;
		}
		if (meth == null) {
			if (other.meth != null) {
				return false;
			}
		} else if (!meth.equals(other.meth)) {
			return false;
		}
		return true;
	}
	public static TaintSource parseTaint(String confline) {
		// Pattern matches "Taint-lines"
		// first part is package name up to space (may not include space or equals sign)
		// second part is Class name [with inner class name] (may not include "=" but may include ".")
		// third part is method name including params (may not include "=" or ".")
		// fourth is index of tainted argument (seperated by "," from the rest)
		Pattern p = Pattern
				.compile("^([^= ]*) ([^=]*)\\.([^=.]*\\([^)]*\\)),([0-9]+)");
		Matcher m = p.matcher(confline);
		if (!m.find()) {
			throw new Error(confline);
		}
		String pack = m.group(1);
		String cls = m.group(2);
		String meth = m.group(3);
		int tgt = Integer.parseInt(m.group(4));
		TaintSource mask = null;
		if (m.end() != confline.length()) {
			String s = confline.substring(m.end(), m.end() + 2);
			if (!s.equals("=>")) {
				throw new Error("malformed");
			}
			mask = parseTaint(confline.substring(m.end() + 2));
		}
		return new TaintSource(pack, cls, meth, tgt, mask);
	}
	public TaintSource getMaskOnly() {
		return maskOnly;
	}
	public int getTgt() {
		return tgt;
	}

}
