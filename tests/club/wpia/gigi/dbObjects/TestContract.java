package club.wpia.gigi.dbObjects;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.testUtils.ClientBusinessTest;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.RandomToken;

public class TestContract extends ClientBusinessTest {

    @Test
    public void testContract() throws GigiApiException {

        assertEquals(Contract.getContractByUser(u, Contract.ContractType.RA_AGENT_CONTRACT), null);

        assertFalse(Contract.hasSignedContract(u, Contract.ContractType.RA_AGENT_CONTRACT));

        Contract c = Contract.getRAAgentContractByUser(u);
        assertEquals(c, null);

        c = new Contract(u, Contract.ContractType.RA_AGENT_CONTRACT);
        TestMail rc = getMailReceiver().receive(u.getEmail());

        assertEquals(u.getEmail(), rc.getTo());
        assertThat(rc.getMessage(), CoreMatchers.containsString("signed the RA Agent Contract"));
        assertEquals(u.getPreferredName().toString(), c.getRAAgentName());
        assertTrue(Contract.hasSignedContract(u, Contract.ContractType.RA_AGENT_CONTRACT));

        Contract c1 = null;
        try {
            c1 = new Contract(u, Contract.ContractType.RA_AGENT_CONTRACT);
            fail("double add contract must fail");
        } catch (GigiApiException e) {
            assertEquals("Contract exists", e.getMessage());
        }

        c1 = Contract.getContractByUser(u, Contract.ContractType.RA_AGENT_CONTRACT);
        assertEquals(c.getID(), c1.getID());

        c1 = Contract.getRAAgentContractByUser(u);
        assertEquals(c.getID(), c1.getID());

        c1 = Contract.getRAAgentContractByToken(c.getToken());
        assertEquals(c.getID(), c1.getID());

        c1 = Contract.getRAAgentContractByToken(RandomToken.generateToken(16));
        assertEquals(c1, null);

    }

    @Test
    public void testRevokeContract() throws GigiApiException {
        Contract c = new Contract(u, Contract.ContractType.RA_AGENT_CONTRACT);

        TestMail rc = getMailReceiver().receive(u.getEmail());
        assertThat(rc.getMessage(), CoreMatchers.containsString("signed the RA Agent Contract"));

        c.revokeContract();

        rc = getMailReceiver().receive(u.getEmail());
        assertEquals(u.getEmail(), rc.getTo());
        assertThat(rc.getMessage(), CoreMatchers.containsString("revoked the RA Agent Contract"));
        assertFalse(Contract.hasSignedContract(u, Contract.ContractType.RA_AGENT_CONTRACT));

        Contract c1 = new Contract(u, Contract.ContractType.RA_AGENT_CONTRACT);
        rc = getMailReceiver().receive(u.getEmail());

        assertNotEquals(c.getID(), c1.getID());
    }

    @Test
    public void testContractInt() throws GigiApiException {
        Contract c = new Contract(u, Contract.ContractType.RA_AGENT_CONTRACT);

        TestMail rc = getMailReceiver().receive(u.getEmail());
        assertThat(rc.getMessage(), CoreMatchers.containsString("signed the RA Agent Contract"));

        Contract c1 = Contract.getById(c.getID());

        assertEquals(c.getID(), c1.getID());
        assertEquals(c.getContractType(), c1.getContractType());

        c1 = Contract.getById(0);
        assertEquals(null, c1);
    }

}
