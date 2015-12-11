package club.wpia.gigi.ocsp;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

import sun.security.provider.certpath.CertId;
import sun.security.x509.AlgorithmId;

/**
 * Idenfies an {@link OCSPIssuer} by remembering its public key hash and its
 * name hash together with the used hash algorithm. A {@link OCSPIssuer} can be
 * identified by several {@link OCSPIssuerId}s when they use different hash
 * algorithms.
 */
public class OCSPIssuerId {

    private final byte[] keyHash;

    private final byte[] nameHash;

    private final AlgorithmId alg;

    /**
     * Creates a new OCSPIssuerId for a given {@link OCSPIssuer}. The hash
     * algorithm has to be specified twice, once for description purposes as
     * {@link AlgorithmId} and once instantiated as {@link MessageDigest}.
     * 
     * @param alg
     *            the description of the hash algorithm
     * @param md
     *            the instantiated hash algorithm
     * @param iss
     *            the issuer to hash.
     */
    public OCSPIssuerId(AlgorithmId alg, MessageDigest md, X509Certificate target) {
        X500Principal dn = target.getSubjectX500Principal();
        this.keyHash = OCSPResponder.calcKeyHash(target, md);
        this.nameHash = md.digest(dn.getEncoded());
        this.alg = alg;
    }

    /**
     * Creates a new OCSPIssuerId from the {@link CertId} inside an OCSP
     * request.
     * 
     * @param id
     *            the {@link CertId}
     */
    public OCSPIssuerId(CertId id) {
        keyHash = id.getIssuerKeyHash();
        nameHash = id.getIssuerNameHash();
        alg = id.getHashAlgorithm();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((alg == null) ? 0 : alg.hashCode());
        result = prime * result + Arrays.hashCode(keyHash);
        result = prime * result + Arrays.hashCode(nameHash);
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
        OCSPIssuerId other = (OCSPIssuerId) obj;
        if (alg == null) {
            if (other.alg != null) {
                return false;
            }
        } else if ( !alg.equals(other.alg)) {
            return false;
        }
        if ( !Arrays.equals(keyHash, other.keyHash)) {
            return false;
        }
        if ( !Arrays.equals(nameHash, other.nameHash)) {
            return false;
        }
        return true;
    }

    public AlgorithmId getAlg() {
        return alg;
    }
}
