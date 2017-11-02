/*
 * Copyright (c) 2017, CRoCS, EnigmaBridge Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/*
 * Credits: ported to Java by Martin Paljak
 */
/*
 * Credits: ported to Gigi KeyCheck interface by Benny Baumann
 */

package club.wpia.gigi.crypto.key;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import club.wpia.gigi.GigiApiException;

/**
 * Due to a bug in several chips produced by Infineon several cryptographic
 * processors in smartcards and other HSM appliances produced RSA keys with a
 * weakness that made them subject for easy factorization. The vulnerability has
 * been present in Infineon's library from about 2014 through 2017 making keys
 * up to 2048 bit RSA practically factorable and strongly weakens larger RSA
 * keys produced with such an implementation. This check implements a
 * fingerprinting mechanism detecting such keys using a heuristic found by the
 * researchers describing the vulnerability. Any such keys SHALL NOT be
 * certified. This implementation is based on the Java port by Martin Paljak.
 *
 * @see <a href="https://crocs.fi.muni.cz/public/papers/rsa_ccs17">Original ROCA
 *      vulnerability website</a>
 * @see <a href="https://arstechnica.com/?post_type=post&p=1186901">Report by
 *      ArsTechnica on ROCA</a>
 * @see <a
 *      href="https://gist.github.com/hannob/ad37d9e9e3cbf3b89bc0a8fc80cb9475">Gist
 *      by Hanno Böck detailling impacted keys</a>
 * @see <a
 *      href="https://github.com/crocs-muni/roca/blob/master/java/BrokenKey.java">Java
 *      port by Martin Paljak</a>
 */
public class KeyCheckROCA extends KeyCheck {

    /**
     * List of Lengths of masks for the listed {@link #markers}.
     */
    private static final int[] prims = new int[] {
            11, 13, 17, 19, 37, 53, 61, 71, 73, 79, 97, 103, 107, 109, 127, 151, 157
    };

    private static final BigInteger[] primes = new BigInteger[prims.length];

    /**
     * List of markers used to fingerprint keys vulnerable to ROCA.
     *
     * This list is reduced according to:
     * {@link https://groups.google.com/d/msg/mozilla.dev.security.policy/4RqKdD0FeF4/DcxMqchSAQAJ}
     */
    private static final BigInteger[] markers = new BigInteger[] {
            new BigInteger("1026"), // 11:402
            new BigInteger("5658"), // 13:161a
            new BigInteger("107286"), // 17:1a316
            new BigInteger("199410"), // 19:30af2
            new BigInteger("67109890"), // 37:4000402
            new BigInteger("5310023542746834"), // 53:12dd703303aed2
            new BigInteger("1455791217086302986"), // 61:1434026619900b0a
            new BigInteger("20052041432995567486"), // 71:1164729716b1d977e
            new BigInteger("6041388139249378920330"), // 73:147811a48004962078a
            new BigInteger("207530445072488465666"), // 79:b4010404000640502
            new BigInteger("79228162521181866724264247298"), // 97:1000000006000001800000002
            new BigInteger("1760368345969468176824550810518"), // 103:16380e9115bd964257768fe396
            new BigInteger("50079290986288516948354744811034"), // 107:27816ea9821633397be6a897e1a
            new BigInteger("473022961816146413042658758988474"), // 109:1752639f4e85b003685cbe7192ba
            new BigInteger("144390480366845522447407333004847678774"), // 127:6ca09850c2813205a04c81430a190536
            new BigInteger("1800793591454480341970779146165214289059119882"), // 151:50c018bc00482458dac35b1a2412003d18030a
            new BigInteger("126304807362733370595828809000324029340048915994"), // 157:161fb414d76af63826461899071bd5baca0b7e1a
    };

    static {
        for (int i = 0; i < prims.length; i++) {
            primes[i] = BigInteger.valueOf(prims[i]);
        }

        register(new KeyCheckROCA());
    }

    @Override
    public void check(PublicKey key) throws GigiApiException {

        if ( !(key instanceof RSAPublicKey)) {
            return;
        }

        BigInteger modulus = ((RSAPublicKey) key).getModulus();

        for (int i = 0; i < primes.length; i++) {
            if (BigInteger.ONE.shiftLeft(modulus.remainder(primes[i]).intValue()).and(markers[i]).equals(BigInteger.ZERO)) {
                return;
            }
        }

        throw new GigiApiException("ROCA vulnerability check for public key: Key likely vulnerable as fingerprint matches.");

    }

}
