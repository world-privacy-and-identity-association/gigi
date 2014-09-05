package org.cacert.gigi.dbObjects;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Outputable;

public class Name implements Outputable, Cloneable {

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
    public String toString() {
        return fname + " " + lname;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fname == null) ? 0 : fname.hashCode());
        result = prime * result + ((lname == null) ? 0 : lname.hashCode());
        result = prime * result + ((mname == null) ? 0 : mname.hashCode());
        result = prime * result + ((suffix == null) ? 0 : suffix.hashCode());
        return result;
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
        Name other = (Name) obj;
        if (fname == null) {
            if (other.fname != null) {
                return false;
            }
        } else if ( !fname.equals(other.fname)) {
            return false;
        }
        if (lname == null) {
            if (other.lname != null) {
                return false;
            }
        } else if ( !lname.equals(other.lname)) {
            return false;
        }
        if (mname == null) {
            if (other.mname != null) {
                return false;
            }
        } else if ( !mname.equals(other.mname)) {
            return false;
        }
        if (suffix == null) {
            if (other.suffix != null) {
                return false;
            }
        } else if ( !suffix.equals(other.suffix)) {
            return false;
        }
        return true;
    }

    public boolean matches(String text) {
        return text.equals(fname + " " + lname) || //
                (mname != null && text.equals(fname + " " + mname + " " + lname)) || //
                (suffix != null && text.equals(fname + " " + lname + " " + suffix)) || //
                (mname != null && suffix != null && text.equals(fname + " " + mname + " " + lname + " " + suffix));
    }

    @Override
    protected Name clone() {
        try {
            return (Name) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

}