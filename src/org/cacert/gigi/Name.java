package org.cacert.gigi;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.output.Outputable;

public class Name implements Outputable {

    String fname;

    String mname;

    String lname;

    String suffix;

    public Name(String fname, String lname, String mname, String suffix) {
        this.fname = fname;
        this.lname = lname;
        this.mname = mname;
        this.suffix = suffix;
    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.println("<span class=\"accountdetail\">");
        out.print("<span class=\"fname\">");
        out.print(fname);
        out.print("</span> ");
        out.print("<span class=\"lname\">");
        out.print(lname);
        out.print("</span>");
        out.println("</span>");
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof Name)) {
            return false;
        }
        Name n = (Name) obj;
        if ( !(n.fname.equals(fname) && n.lname.equals(lname))) {
            return false;
        }
        if (mname == null) {
            if (n.mname != null) {
                return false;
            }
        } else if ( !mname.equals(n.mname)) {
            return false;
        }
        if (suffix == null) {
            if (n.suffix != null) {
                return false;
            }
        } else if ( !suffix.equals(n.suffix)) {
            return false;
        }
        return true;

    }
}
