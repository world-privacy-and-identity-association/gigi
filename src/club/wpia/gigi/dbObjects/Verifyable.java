package club.wpia.gigi.dbObjects;

import club.wpia.gigi.GigiApiException;

public interface Verifyable {

    public void verify(String hash) throws GigiApiException;

    public boolean isVerifyable(String hash) throws GigiApiException;

}
