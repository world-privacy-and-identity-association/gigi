package club.wpia.gigi.dbObjects;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.DBEnum;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.MailTemplate;
import club.wpia.gigi.util.RandomToken;

public class Contract {

    public enum ContractType implements DBEnum {
        RA_AGENT_CONTRACT("RA Agent Contract"), ORG_RA_AGENT_CONTRACT("Org RA Agent Contract");

        private final String description;

        private ContractType(String description) {
            this.description = description;
        }

        public String getDBName() {
            return description;
        }
    }

    private final ContractType contractType;

    private final User user;

    private String agentname = "";

    private String token = "";

    private Date dateSigned = null;

    private Date dateRevoked = null;

    private int contractID;

    private static final MailTemplate contractNotice = new MailTemplate(Contract.class.getResource("ContractNotice.templ"));

    public Contract(User u, ContractType contractType) throws GigiApiException {
        this.contractType = contractType;
        this.user = u;
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT * FROM `user_contracts` WHERE `memid`=? AND `document`=?::`contractType` and `daterevoked` IS NULL ORDER BY `datesigned` DESC LIMIT 1")) {
            query.setInt(1, user.getId());
            query.setEnum(2, contractType);
            GigiResultSet rs = query.executeQuery();
            if (rs.next()) {
                throw new GigiApiException("Contract exists");
            } else {
                signContract();
            }
        }

    }

    private void signContract() throws GigiApiException {
        agentname = user.getPreferredName().toString();
        token = RandomToken.generateToken(32);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `user_contracts` SET `memid`=?, `token`=?, `document`=?::`contractType`,`agentname`=?")) {
            ps.setInt(1, user.getId());
            ps.setString(2, token);
            ps.setEnum(3, this.contractType);
            ps.setString(4, agentname);
            ps.execute();
            contractID = ps.lastInsertId();
            dateSigned = new Date();

            HashMap<String, Object> vars = new HashMap<>();
            Language l = Language.getInstance(user.getPreferredLocale());
            vars.put("user", agentname);
            vars.put("actionsubject", "Signing");
            vars.put("actionbody", "signed");

            try {
                contractNotice.sendMail(l, vars, user.getEmail());
            } catch (IOException e) {
                throw new GigiApiException("Sending the notification mail failed.");
            }
        }
    }

    public void revokeContract() throws GigiApiException {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("UPDATE `user_contracts` SET `daterevoked`=NOW() WHERE `id`=?")) {
            ps.setInt(1, contractID);
            ps.execute();
        }
        dateRevoked = new Date();
        HashMap<String, Object> vars = new HashMap<>();
        Language l = Language.getInstance(user.getPreferredLocale());
        vars.put("user", user.getPreferredName());
        vars.put("actionsubject", "Revoking");
        vars.put("actionbody", "revoked");

        try {
            contractNotice.sendMail(l, vars, user.getEmail());
        } catch (IOException e) {
            throw new GigiApiException("Sending the notification mail failed.");
        }
    }

    private Contract(GigiResultSet rs) {
        contractID = rs.getInt("id");
        user = User.getById(rs.getInt("memid"));
        token = rs.getString("token");
        contractType = ContractType.valueOf(rs.getString("document").toUpperCase().replace(" ", "_"));
        dateSigned = rs.getDate("datesigned");
        dateRevoked = rs.getDate("daterevoked");
        agentname = rs.getString("agentname");
    }

    public static Contract getById(int id) {
        try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT * FROM `user_contracts` WHERE `id` = ?")) {
            ps.setInt(1, id);
            GigiResultSet rs = ps.executeQuery();
            if ( !rs.next()) {
                return null;
            }

            Contract c = new Contract(rs);

            return c;
        }
    }

    public int getID() {
        return contractID;
    }

    public Date getDateSigned() {
        return dateSigned;
    }

    public Date getDateRevoked() {
        return dateRevoked;
    }

    public String getRAAgentName() {
        return agentname;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public String getToken() {
        return token;
    }

    public static boolean hasSignedContract(User u, Contract.ContractType ct) {
        return getContractByUser(u, ct) != null;
    }

    public static Contract getRAAgentContractByUser(User u) {
        return getContractByUser(u, Contract.ContractType.RA_AGENT_CONTRACT);
    }

    public static Contract getContractByUser(User u, ContractType ct) {
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT * FROM `user_contracts` WHERE `memid`=? AND `document`=?::`contractType` and `daterevoked` IS NULL ORDER BY `datesigned` DESC LIMIT 1")) {
            query.setInt(1, u.getId());
            query.setEnum(2, ct);
            GigiResultSet rs = query.executeQuery();
            if ( !rs.next()) {
                return null;
            }
            Contract c = new Contract(rs);
            return c;
        }
    }

    public static Contract getRAAgentContractByToken(String token) throws GigiApiException {
        try (GigiPreparedStatement query = new GigiPreparedStatement("SELECT * FROM `user_contracts` WHERE `token`=? LIMIT 1")) {
            query.setString(1, token);
            GigiResultSet rs = query.executeQuery();
            if ( !rs.next()) {
                return null;
            }
            Contract c = new Contract(rs);
            return c;
        }
    }
}
