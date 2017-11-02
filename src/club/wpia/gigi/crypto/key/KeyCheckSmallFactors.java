package club.wpia.gigi.crypto.key;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.output.template.SprintfCommand;

public class KeyCheckSmallFactors extends KeyCheck {

    private static final long MAX_CHECKED_SMALL_PRIME_BOUNDARY = 10000;

    private static final BigInteger[] primes;

    static {
        ArrayList<BigInteger> prims = new ArrayList<>(1024);

        NextPrime:
        for (long i = 2; i < MAX_CHECKED_SMALL_PRIME_BOUNDARY; i++) {
            for (BigInteger p : prims) {
                if (BigInteger.ZERO.equals(BigInteger.valueOf(i).mod(p))) {
                    continue NextPrime;
                }
            }

            prims.add(BigInteger.valueOf(i));
        }

        primes = prims.toArray(new BigInteger[0]);

        register(new KeyCheckSmallFactors());
    }

    @Override
    public void check(PublicKey key) throws GigiApiException {
        if ( !(key instanceof RSAPublicKey)) {
            return;
        }

        BigInteger modulus = ((RSAPublicKey) key).getModulus();

        // Check for small prime factors below 10000
        for (BigInteger n : primes) {
            if (BigInteger.ZERO.equals(modulus.mod(n))) {
                throw new GigiApiException(SprintfCommand.createSimple("Small factors check of public key: Key is divisible by {0}.", n.toString()));
            }
        }
    }

}
