package club.wpia.gigi;

public class TestLauncher {

    public static void main(String[] args) throws Exception {
        // As clean as possible
        Launcher.main(args);
        DevelLauncher.addDevelPage(false);
        System.err.println("System successfully started.");

    }
}
