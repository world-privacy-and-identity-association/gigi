package club.wpia.gigi.testUtils;

import java.util.regex.Pattern;

import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;

public interface MailReceiver {

    void assertEmpty();

    TestMail receive(String to);

    void setApproveRegex(Pattern compiled);

    void setEmailCheckError(String string);

    TestMail poll(String to);

}
