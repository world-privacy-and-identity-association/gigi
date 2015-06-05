package org.cacert.gigi.dbObjects;

import org.cacert.gigi.GigiApiException;

public interface Verifyable {

    public void verify(String hash) throws GigiApiException;

}
