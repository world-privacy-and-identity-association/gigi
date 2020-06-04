package club.wpia.gigi;

import java.util.Properties;

import org.junit.BeforeClass;

import club.wpia.gigi.testUtils.ManagedTest;

public class TestCommunityCAOn extends TestCommunityCAOff {

    @BeforeClass
    public static void initEnvironmentHook() {
        Properties additionalConfig = new Properties();
        additionalConfig.setProperty("communityCA", "true");
        isCommunityCATest = true;
        ManagedTest.initEnvironment(additionalConfig);
    }

}
