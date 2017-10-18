package club.wpia.gigi.crypto.key;

import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import club.wpia.gigi.GigiApiException;

public abstract class KeyCheck {

    protected static final Set<KeyCheck> checks = new LinkedHashSet<KeyCheck>();

    public static List<KeyCheck> getChecks() {
        return Collections.list(Collections.enumeration(checks));
    }

    public static void register(KeyCheck check) {
        checks.add(check);
    }

    public abstract void check(PublicKey key) throws GigiApiException;

    public static void checkKey(PublicKey key) throws GigiApiException {

        if (checks.isEmpty() || checks.size() < 1) {
            // Mandatory checks are registered here
            register(new KeyCheckSmallFactors());
        }

        if (key == null) {
            throw new GigiApiException("Failed key sanity check: No key given!");
        }

        for (KeyCheck kc : checks) {
            kc.check(key);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        return getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
