package club.wpia.gigi.dbObjects;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.DBEnum;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.NamePart.NamePartType;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.util.HTMLEncoder;
import club.wpia.gigi.util.TimeConditions;

public class Name implements Outputable, IdCachable {

    public static enum NameSchemaType implements DBEnum {
        SINGLE, WESTERN;

        @Override
        public String getDBName() {
            return toString().toLowerCase();
        }

    }

    private abstract static class SchemedName {

        /**
         * @see Name#matches(String)
         */
        public abstract boolean matches(String text);

        public abstract String toPreferredString();

        /**
         * @see Name#toAbbreviatedString()
         */
        public abstract String toAbbreviatedString();

        public abstract NameSchemaType getSchemeName();

        /**
         * @see Name#output(PrintWriter, Language, Map)
         */
        public abstract void output(PrintWriter out);

        /**
         * @see Name#toInitialsString()
         */
        public abstract String toInitialsString();

    }

    private static class SingleName extends SchemedName {

        private NamePart singlePart;

        public SingleName(NamePart singlePart) {
            this.singlePart = singlePart;
        }

        @Override
        public boolean matches(String text) {
            return text.equals(singlePart.getValue());
        }

        @Override
        public String toPreferredString() {
            return singlePart.getValue();
        }

        @Override
        public String toAbbreviatedString() {
            return singlePart.getValue();
        }

        @Override
        public String toInitialsString() {
            return singlePart.getValue().substring(0, 1);
        }

        @Override
        public NameSchemaType getSchemeName() {
            return NameSchemaType.SINGLE;
        }

        @Override
        public void output(PrintWriter out) {
            out.print("<span class='sname'>");
            out.print(HTMLEncoder.encodeHTML(singlePart.getValue()));
            out.print("</span>");
        }
    }

    /**
     * Naming scheme where any first name and the first last name is required.
     * Requires first names in arbitrary order. Last names and suffixes in
     * correct order.
     */
    private static class WesternName extends SchemedName {

        private NamePart[] firstNames;

        private NamePart[] lastNames;

        private NamePart[] suffixes;

        public WesternName(NamePart[] firstName, NamePart lastName[], NamePart[] suffixes) {
            if (lastName.length < 1 || firstName.length < 1) {
                throw new Error("Requires at least one first and one last name");
            }
            this.lastNames = lastName;
            this.firstNames = firstName;
            this.suffixes = suffixes;
        }

        @Override
        public boolean matches(String text) {
            String[] tokens = text.split(" ");

            NamePart mandatoryLN = lastNames[0];
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals(mandatoryLN.getValue())) {
                    if (tryMatchFirst(tokens, i) && tryMatchLastSuff(tokens, i)) {
                        return true;
                    }

                }
            }
            return false;
        }

        private boolean tryMatchLastSuff(String[] tokens, int lastName) {
            int userInputPos = lastName + 1;
            boolean currentlyMatchingLastNames = true;
            int referencePos = 1;
            while ((currentlyMatchingLastNames || referencePos < suffixes.length) && (userInputPos < tokens.length)) {
                // we break when we match suffixes and there is no
                // reference-suffix left
                if (currentlyMatchingLastNames) {
                    if (referencePos >= lastNames.length) {
                        referencePos = 0;
                        currentlyMatchingLastNames = false;
                    } else if (tokens[userInputPos].equals(lastNames[referencePos].getValue())) {
                        userInputPos++;
                        referencePos++;
                    } else {
                        referencePos++;
                    }
                } else {
                    if (tokens[userInputPos].equals(suffixes[referencePos].getValue())) {
                        userInputPos++;
                        referencePos++;
                    } else {
                        referencePos++;
                    }
                }
            }
            if (userInputPos >= tokens.length) {
                // all name parts are covered we're done here
                return true;
            }
            return false;
        }

        private boolean tryMatchFirst(String[] tokens, int lastName) {
            if (lastName == 0) {
                return false;
            }
            boolean[] fnUsed = new boolean[firstNames.length];
            for (int i = 0; i < lastName; i++) {
                boolean found = false;
                for (int j = 0; j < fnUsed.length; j++) {
                    if ( !fnUsed[j] && firstNames[j].getValue().equals(tokens[i])) {
                        fnUsed[j] = true;
                        found = true;
                        break;
                    }
                }
                if ( !found) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toPreferredString() {
            StringBuilder res = new StringBuilder();
            appendArray(res, firstNames);
            appendArray(res, lastNames);
            appendArray(res, suffixes);
            res.deleteCharAt(res.length() - 1);
            return res.toString();
        }

        @Override
        public void output(PrintWriter out) {
            outputNameParts(out, "fname", firstNames, false);
            outputNameParts(out, "lname", lastNames, true);
            outputNameParts(out, "suffix", suffixes, true);
        }

        private void outputNameParts(PrintWriter out, String type, NamePart[] input, boolean leadingSpace) {
            StringBuilder res;
            res = new StringBuilder();
            appendArray(res, input);
            if (res.length() > 0) {
                res.deleteCharAt(res.length() - 1);
                if (leadingSpace) {
                    out.print(" ");
                }
                out.print("<span class='" + type + "'>");
                out.print(HTMLEncoder.encodeHTML(res.toString()));
                out.print("</span>");
            }
        }

        private void appendArray(StringBuilder res, NamePart[] ps) {
            for (int i = 0; i < ps.length; i++) {
                res.append(ps[i].getValue());
                res.append(" ");
            }
        }

        @Override
        public NameSchemaType getSchemeName() {
            return NameSchemaType.WESTERN;
        }

        @Override
        public String toAbbreviatedString() {
            return firstNames[0].getValue() + " " + lastNames[0].getValue().charAt(0) + ".";
        }

        @Override
        public String toInitialsString() {

            String initals = getInitialByNamePart(firstNames, lastNames, suffixes);

            return initals;
        }
    }

    private int id;

    private int ownerId;

    /**
     * Only resolved lazily to resolve circular referencing with {@link User} on
     * {@link User#getPreferredName()}. Resolved based on {@link #ownerId}.
     */
    private User owner;

    private NamePart[] parts;

    private SchemedName scheme;

    /**
     * This name should not get verifed anymore and therefore not be displayed
     * to the RA-Agent. This state is irrevocable.
     */
    private boolean deprecated;

    private Name(GigiResultSet rs) {
        ownerId = rs.getInt(1);
        id = rs.getInt(2);
        deprecated = rs.getString("deprecated") != null;
        try (GigiPreparedStatement partFetcher = new GigiPreparedStatement("SELECT `type`, `value` FROM `nameParts` WHERE `id`=? ORDER BY `position` ASC", true)) {
            partFetcher.setInt(1, id);
            GigiResultSet rs1 = partFetcher.executeQuery();
            rs1.last();
            NamePart[] dt = new NamePart[rs1.getRow()];
            rs1.beforeFirst();
            for (int i = 0; rs1.next(); i++) {
                dt[i] = new NamePart(rs1);
            }
            parts = dt;
            scheme = detectScheme();
        }

    }

    public Name(User u, NamePart... np) throws GigiApiException {
        synchronized (Name.class) {
            parts = np;
            owner = u;
            scheme = detectScheme();
            if (scheme == null) {
                throw new GigiApiException("Name particles don't match up for any known name scheme.");
            }
            try (GigiPreparedStatement inserter = new GigiPreparedStatement("INSERT INTO `names` SET `uid`=?, `type`=?::`nameSchemaType`")) {
                inserter.setInt(1, u.getId());
                inserter.setEnum(2, scheme.getSchemeName());
                inserter.execute();
                id = inserter.lastInsertId();
            }
            try (GigiPreparedStatement inserter = new GigiPreparedStatement("INSERT INTO `nameParts` SET `id`=?, `position`=?, `type`=?::`namePartType`, `value`=?")) {
                inserter.setInt(1, id);
                for (int i = 0; i < np.length; i++) {
                    inserter.setInt(2, i);
                    inserter.setEnum(3, np[i].getType());
                    inserter.setString(4, np[i].getValue());
                    inserter.execute();
                }
            }
            cache.put(this);
        }
    }

    private SchemedName detectScheme() {
        if (parts.length == 1 && parts[0].getType() == NamePartType.SINGLE_NAME) {
            return new SingleName(parts[0]);
        }
        int suffixCount = 0;
        int lastCount = 0;
        int firstCount = 0;
        int stage = 0;
        for (NamePart p : parts) {
            if (p.getType() == NamePartType.LAST_NAME) {
                lastCount++;
                if (stage < 1) {
                    stage = 1;
                } else if (stage != 1) {
                    return null;
                }
            } else if (p.getType() == NamePartType.FIRST_NAME) {
                firstCount++;
                if (stage != 0) {
                    return null;
                }
            } else if (p.getType() == NamePartType.SUFFIX) {
                suffixCount++;
                if (stage < 2) {
                    stage = 2;
                } else if (stage != 2) {
                    return null;
                }

            } else {
                return null;
            }
        }
        if (firstCount == 0 || lastCount == 0) {
            return null;
        }
        NamePart[] firstNames = new NamePart[firstCount];
        NamePart[] lastNames = new NamePart[lastCount];
        NamePart[] suffixes = new NamePart[suffixCount];
        int fn = 0;
        int ln = 0;
        int sn = 0;
        for (NamePart p : parts) {
            if (p.getType() == NamePartType.FIRST_NAME) {
                firstNames[fn++] = p;
            } else if (p.getType() == NamePartType.SUFFIX) {
                suffixes[sn++] = p;
            } else if (p.getType() == NamePartType.LAST_NAME) {
                lastNames[ln++] = p;
            }
        }

        return new WesternName(firstNames, lastNames, suffixes);
    }

    /**
     * Outputs an HTML variant suitable for locations where special UI features
     * should indicate the different Name Parts.
     */
    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print("<span class=\"names\">");
        scheme.output(out);
        out.print("</span>");
    }

    /**
     * Tests, if this name fits into the given string.
     * 
     * @param text
     *            the name to test against
     * @return true, iff this name matches.
     */
    public boolean matches(String text) {
        if ( !text.equals(text.trim())) {
            return false;
        }
        return scheme.matches(text);
    }

    @Override
    public String toString() {
        return scheme.toPreferredString();
    }

    /**
     * Transforms this String into a short form. This short form should not be
     * unique. (For "western" names this would be "firstName
     * firstCharOfLastName.".)
     * 
     * @return the short form of the name
     */
    public String toAbbreviatedString() {
        return scheme.toAbbreviatedString();
    }

    /**
     * Transforms this Name object into a short form. This short form might not
     * be unique. (For "western" names this would be all first letters of each
     * name part)
     * 
     * @return the short form of the name
     */
    public String toInitialsString() {
        return scheme.toInitialsString();
    }

    public int getVerificationPoints() {
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT SUM(`points`) FROM (SELECT DISTINCT ON (`from`, `method`) `points` FROM `notary` WHERE `to`=? AND `deleted` IS NULL AND (`expire` IS NULL OR `expire` > CURRENT_TIMESTAMP) ORDER BY `from`, `method`, `when` DESC) AS p")) {
            query.setInt(1, getId());

            GigiResultSet rs = query.executeQuery();
            int points = 0;

            if (rs.next()) {
                points = rs.getInt(1);
            }

            return points;
        }
    }

    @Override
    public int getId() {
        return id;
    }

    private static ObjectCache<Name> cache = new ObjectCache<>();

    public synchronized static Name getById(int id) {
        Name cacheRes = cache.get(id);
        if (cacheRes != null) {
            return cacheRes;
        }

        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `uid`, `id`, `deprecated` FROM `names` WHERE `deleted` IS NULL AND `id` = ?")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                return null;
            }

            Name c = new Name(rs);
            cache.put(c);
            return c;
        }
    }

    public NamePart[] getParts() {
        return parts;
    }

    public void remove() {
        synchronized (Name.class) {
            cache.remove(this);
            try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `names` SET `deleted` = now() WHERE `id`=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    public synchronized void deprecate() {
        deprecated = true;
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `names` SET `deprecated`=now() WHERE `id`=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public synchronized User getOwner() {
        if (owner == null) {
            owner = User.getById(ownerId);
        }
        return owner;
    }

    private static String getInitialByNamePart(NamePart[]... npa) {
        StringBuilder initals = new StringBuilder();
        for (NamePart[] np : npa) {
            initals.append(getInitialByNamePart(np));
        }
        return initals.toString();
    }

    private static String getInitialByNamePart(NamePart[] np) {
        StringBuilder initals = new StringBuilder();
        for (NamePart p : np) {
            switch (p.getValue()) {
            case "-":
            case "/":
                break;
            default:
                initals.append(p.getValue().substring(0, 1).toUpperCase());
                break;
            }
        }
        return initals.toString();
    }

    public boolean isValidVerification() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.MONTH, -TimeConditions.getInstance().getVerificationMonths());
        String date = sdf.format(new Date(c.getTimeInMillis()));
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT COUNT(id) FROM `notary` WHERE `to` = ? AND `deleted` IS NULL AND (`expire` IS NULL OR `expire` > CURRENT_TIMESTAMP) AND `date` > ?")) {
            query.setInt(1, getId());
            query.setString(2, date);
            GigiResultSet rs = query.executeQuery();

            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }

            return false;
        }
    }

}
