package club.wpia.gigi.crypto.key;

import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.output.template.SprintfCommand;

public class KeyCheckPublicKeyFormat extends KeyCheck {

    static {
        register(new KeyCheckPublicKeyFormat());
    }

    @Override
    public void check(PublicKey key) throws GigiApiException {

        if (key instanceof RSAPublicKey) {
            return;
        }

        if (key instanceof DSAPublicKey) {
            return;
        }

        if (key instanceof ECPublicKey) {
            return;
        }

        throw new GigiApiException(SprintfCommand.createSimple("Public Key Format Check: Unknown or unsupported public key algorithm {0}", key.getAlgorithm()));

    }

}
