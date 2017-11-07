package club.wpia.gigi.crypto.key;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.output.template.SprintfCommand;

public class KeyCheckSmallFactors extends KeyCheck {

    private static final long MAX_CHECKED_SMALL_PRIME_BOUNDARY = 10000;

    private static final BigInteger primeProduct;

    static {
        BigInteger prod = BigInteger.ONE;

        NextPrime:
        for (long i = 2; i < MAX_CHECKED_SMALL_PRIME_BOUNDARY; i++) {
            if ( !BigInteger.ONE.equals(BigInteger.valueOf(i).gcd(prod))) {
                continue NextPrime;
            }

            prod = prod.multiply(BigInteger.valueOf(i));
        }

        primeProduct = prod;

        register(new KeyCheckSmallFactors());
    }

    @Override
    public void check(PublicKey key) throws GigiApiException {
        if ( !(key instanceof RSAPublicKey)) {
            return;
        }

        BigInteger modulus = ((RSAPublicKey) key).getModulus();

        // Check for small prime factors below 10000
        BigInteger n = modulus.gcd(primeProduct);
        if ( !BigInteger.ONE.equals(n)) {
            throw new GigiApiException(SprintfCommand.createSimple("Small factors check of public key: Key has known factor of {0}.", n.toString()));
        }
    }

}
