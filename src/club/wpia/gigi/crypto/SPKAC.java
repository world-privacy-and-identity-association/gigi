package club.wpia.gigi.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.SignatureException;

import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X509Key;

/**
 * This class handles a SPKAC. A SPKAC has the following structure;
 * 
 * <pre>
 * PublicKeyAndChallenge ::= SEQUENCE {
 *     spki SubjectPublicKeyInfo,
 *     challenge IA5STRING
 * }
 * 
 * SignedPublicKeyAndChallenge ::= SEQUENCE {
 *     publicKeyAndChallenge PublicKeyAndChallenge,
 *     signatureAlgorithm AlgorithmIdentifier,
 *     signature BIT STRING
 * }
 * </pre>
 */
public class SPKAC {

    private X509Key pubkey;

    private String challenge;

    public SPKAC(byte[] data) throws IOException, GeneralSecurityException {
        DerInputStream derIn = new DerInputStream(data);

        DerValue derSPKACContent[] = derIn.getSequence(3);
        if (derIn.available() != 0) {
            throw new IllegalArgumentException("Additional data after SPKAC.");
        }

        AlgorithmId id = AlgorithmId.parse(derSPKACContent[1]);

        derIn = derSPKACContent[0].toDerInputStream();

        pubkey = (X509Key) X509Key.parse(derIn.getDerValue());

        DerValue derChallenge = derIn.getDerValue();
        if (derIn.available() != 0) {
            throw new IllegalArgumentException("Additional data after SPKAC.");
        }
        if (derChallenge.length() != 0) {
            challenge = derChallenge.getIA5String();
        }

        Signature s = Signature.getInstance(id.getName());
        s.initVerify(pubkey);
        s.update(derSPKACContent[0].toByteArray());
        byte[] signature = derSPKACContent[2].getBitString();
        if ( !s.verify(signature)) {
            throw new SignatureException();
        }

    }

    public String getChallenge() {
        return challenge;
    }

    public X509Key getPubkey() {
        return pubkey;
    }

    public SPKAC(X509Key pubkey, String challenge) {
        this.pubkey = pubkey;
        this.challenge = challenge;
    }

    public byte[] getEncoded(Signature sign) throws GeneralSecurityException, IOException {
        DerOutputStream SPKAC = new DerOutputStream();
        DerOutputStream SPKI = new DerOutputStream();

        pubkey.encode(SPKI);
        SPKI.putIA5String(challenge);

        SPKAC.write(DerValue.tag_Sequence, SPKI);
        byte[] toSign = SPKAC.toByteArray();
        SPKI.close();

        AlgorithmId aid = AlgorithmId.get(sign.getAlgorithm());
        aid.encode(SPKAC);
        sign.update(toSign);
        SPKAC.putBitString(sign.sign());

        DerOutputStream res = new DerOutputStream();
        res.write(DerValue.tag_Sequence, SPKAC);
        SPKAC.close();
        byte[] result = res.toByteArray();
        res.close();
        return result;
    }
}
