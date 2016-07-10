package org.cacert.gigi.testUtils;

import java.util.regex.Pattern;

import org.cacert.gigi.testUtils.TestEmailReceiver.TestMail;

public interface MailReceiver {

    void clearMails();

    TestMail receive();

    void setApproveRegex(Pattern compiled);

    void setEmailCheckError(String string);

    TestMail poll();

}
