package club.wpia.gigi.testUtils;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import club.wpia.gigi.DevelLauncher;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.database.GigiPreparedStatement;
import club.wpia.gigi.database.GigiResultSet;
import club.wpia.gigi.dbObjects.Certificate;
import club.wpia.gigi.dbObjects.Certificate.CSRType;
import club.wpia.gigi.dbObjects.Digest;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Job;
import club.wpia.gigi.dbObjects.ObjectCache;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.pages.account.MyDetails;
import club.wpia.gigi.pages.main.RegisterPage;
import club.wpia.gigi.testUtils.TestEmailReceiver.TestMail;
import club.wpia.gigi.util.SimpleSigner;

/**
 * Base class for test suites who require a launched Gigi instance. The instance
 * is cleared once per test suite.
 */
public class ManagedTest extends ConfiguredTest {

    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    private static TestEmailReceiver ter;

    private static Process gigi;

    private static String url = "localhost:4443";

    private static String acceptLanguage = null;

    protected static Certificate loginCertificate;

    protected static PrivateKey loginPrivateKey;

    public static void setAcceptLanguage(String acceptLanguage) {
        ManagedTest.acceptLanguage = acceptLanguage;
    }

    public static String getServerName() {
        return url.replaceFirst(":443$", "");
    }

    public static String getSecureServerName() {
        return getServerName().replaceAll("^www\\.", "secure.");
    }

    static {
        InitTruststore.run();
        HttpURLConnection.setFollowRedirects(false);
    }

    @BeforeClass
    public static void initEnvironmentHook() {
        initEnvironment();
    }

    private static boolean inited = false;

    public static Properties initEnvironment() {
        return initEnvironment(new Properties());
    }

    public static Properties initEnvironment(Properties additionalConfig) {
        try {
            Properties mainProps = ConfiguredTest.initEnvironment();
            for (Entry<Object, Object> i : additionalConfig.entrySet()) {
                if (i.getKey() instanceof String && i.getValue() instanceof String) {
                    mainProps.setProperty((String) i.getKey(), (String) i.getValue());
                }
            }
            if (inited) {
                return mainProps;
            }
            inited = true;
            url = testProps.getProperty("name.www") + ":" + testProps.getProperty("serverPort.https");
            purgeDatabase();
            String type = testProps.getProperty("type");
            generateMainProps(mainProps);
            if (type.equals("local")) {
                String[] parts = testProps.getProperty("mail").split(":", 2);
                ter = new TestEmailReceiver(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
                ter.start();
                if (testProps.getProperty("withSigner", "false").equals("true")) {
                    SimpleSigner.runSigner();
                }
                return mainProps;
            }
            gigi = Runtime.getRuntime().exec(testProps.getProperty("java"));
            DataOutputStream toGigi = new DataOutputStream(gigi.getOutputStream());
            System.out.println("... starting server");

            byte[] cacerts = Files.readAllBytes(Paths.get("config/cacerts.jks"));
            byte[] keystore = Files.readAllBytes(Paths.get("config/keystore.pkcs12"));

            DevelLauncher.writeGigiConfig(toGigi, "changeit".getBytes("UTF-8"), "changeit".getBytes("UTF-8"), mainProps, cacerts, keystore);
            toGigi.flush();

            final BufferedReader br = new BufferedReader(new InputStreamReader(gigi.getErrorStream(), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null && !line.contains("System successfully started.")) {
                System.err.println(line);
            }
            new Thread() {

                @Override
                public void run() {
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            System.err.println(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            if (line == null) {
                throw new Error("Server startup failed");
            }
            ter = new TestEmailReceiver(new InetSocketAddress("localhost", 8473));
            ter.start();
            SimpleSigner.runSigner();
            return mainProps;
        } catch (IOException e) {
            throw new Error(e);
        } catch (SQLException e1) {
            throw new Error(e1);
        } catch (InterruptedException e) {
            throw new Error(e);
        }

    }

    protected static void await(Job j) throws InterruptedException {
        SimpleSigner.ping();
        j.waitFor(5000);
    }

    public static void purgeDatabase() throws SQLException, IOException {
        purgeOnlyDB();
        if (gigi != null) {
            clearCaches();
        }
    }

    public static void clearCaches() throws IOException {
        ObjectCache.clearAllCaches();
        // String type = testProps.getProperty("type");
        URL u = new URL("https://" + getServerName() + "/manage");
        URLConnection connection = u.openConnection();
        connection.getHeaderField("Location");
        connection.getInputStream().close();
    }

    private static void generateMainProps(Properties mainProps) {
        mainProps.setProperty("testrunner", "true");
        mainProps.setProperty("host", "127.0.0.1");

        mainProps.setProperty("emailProvider", "club.wpia.gigi.email.TestEmailProvider");
        mainProps.setProperty("emailProvider.port", "8473");
        mainProps.setProperty("sql.driver", testProps.getProperty("sql.driver"));
        mainProps.setProperty("sql.url", testProps.getProperty("sql.url"));
        mainProps.setProperty("sql.user", testProps.getProperty("sql.user"));
        mainProps.setProperty("sql.password", testProps.getProperty("sql.password"));
        mainProps.setProperty("testing", "true");
    }

    @AfterClass
    public static void tearDownServer() {
        String type = testProps.getProperty("type");
        ter.destroy();
        if (type.equals("local")) {
            if (testProps.getProperty("withSigner", "false").equals("true")) {
                try {
                    SimpleSigner.stopSigner();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            inited = false;
            return;
        }
        gigi.destroy();
        try {
            SimpleSigner.stopSigner();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        inited = false;
    }

    public final String uniq = createUniqueName();

    @After
    public void removeMails() {
        ter.reset();
    }

    @After
    public void clearAcceptLanguage() {
        ManagedTest.setAcceptLanguage(null);
    }

    @Override
    public MailReceiver getMailReceiver() {
        return ter;
    }

    public static String runRegister(String param) throws IOException {
        URL regist = new URL("https://" + getServerName() + RegisterPage.PATH);
        HttpURLConnection uc = (HttpURLConnection) regist.openConnection();
        HttpURLConnection csrfConn = (HttpURLConnection) regist.openConnection();
        if (acceptLanguage != null) {
            csrfConn.setRequestProperty("Accept-Language", acceptLanguage);
            uc.setRequestProperty("Accept-Language", acceptLanguage);
        }

        String headerField = csrfConn.getHeaderField("Set-Cookie");
        headerField = stripCookie(headerField);

        String csrf = getCSRF(csrfConn);
        uc.addRequestProperty("Cookie", headerField);
        uc.setDoOutput(true);
        uc.getOutputStream().write((param + "&csrf=" + csrf).getBytes("UTF-8"));
        if (uc.getResponseCode() == 302) {
            return "";
        }
        String d = IOUtils.readURL(uc);
        return d;
    }

    public static org.hamcrest.Matcher<String> hasError() {
        return CoreMatchers.containsString("<div class='alert alert-danger error-msgs'>");
    }

    public static org.hamcrest.Matcher<String> hasNoError() {
        return CoreMatchers.not(hasError());
    }

    public static String fetchStartErrorMessage(String d) throws IOException {
        String formFail = "<div class='alert alert-danger error-msgs'>";
        int idx = d.indexOf(formFail);
        if (idx == -1) {
            return null;
        }
        String startError = d.substring(idx + formFail.length(), idx + formFail.length() + 150).trim();
        return startError;
    }

    public static void registerUser(String firstName, String lastName, String email, String password) {
        try {
            String query = "name-type=western&fname=" + URLEncoder.encode(firstName, "UTF-8") + "&lname=" + URLEncoder.encode(lastName, "UTF-8") + "&email=" + URLEncoder.encode(email, "UTF-8") + "&pword1=" + URLEncoder.encode(password, "UTF-8") + "&pword2=" + URLEncoder.encode(password, "UTF-8") + "&day=1&month=1&year=1910&tos_agree=1&dp_agree=1";
            String data = fetchStartErrorMessage(runRegister(query));
            assertNull(data);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static int createVerifiedUser(String firstName, String lastName, String email, String password) {
        registerUser(firstName, lastName, email, password);
        try {
            ter.receive(email).verify();

            try (GigiPreparedStatement ps = new GigiPreparedStatement("SELECT `id` FROM `users` WHERE `email`=?")) {
                ps.setString(1, email);

                GigiResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new Error();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static void grant(User u, Group g) throws IOException, GigiApiException {
        u.grantGroup(getSupporter(), g);
        clearCaches();
    }

    /**
     * Creates a new user with 100 Verification Points given by an (invalid)
     * verification.
     * 
     * @param firstName
     *            the first name
     * @param lastName
     *            the last name
     * @param email
     *            the email
     * @param password
     *            the password
     * @return a new userid.
     */
    public static int createVerificationUser(String firstName, String lastName, String email, String password) {
        int uid = createVerifiedUser(firstName, lastName, email, password);

        makeAgent(uid);

        return uid;
    }

    protected static String stripCookie(String headerField) {
        return headerField.substring(0, headerField.indexOf(';'));
    }

    public static final String SECURE_REFERENCE = MyDetails.PATH;

    public static boolean isLoggedin(String cookie) throws IOException {
        URL u = new URL("https://" + getServerName() + SECURE_REFERENCE);
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();
        huc.addRequestProperty("Cookie", cookie);
        return huc.getResponseCode() == 200;
    }

    public static String login(String email, String pw) throws IOException {
        URL u = new URL("https://" + getServerName() + "/login");
        HttpURLConnection huc = (HttpURLConnection) u.openConnection();

        String csrf = getCSRF(huc);
        String headerField = stripCookie(huc.getHeaderField("Set-Cookie"));

        huc = (HttpURLConnection) u.openConnection();
        cookie(huc, headerField);
        huc.setDoOutput(true);
        OutputStream os = huc.getOutputStream();
        String data = "username=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(pw, "UTF-8") + "&csrf=" + URLEncoder.encode(csrf, "UTF-8");
        os.write(data.getBytes("UTF-8"));
        os.flush();
        headerField = huc.getHeaderField("Set-Cookie");
        if (headerField == null) {
            return "";
        }
        if (huc.getResponseCode() != 302) {
            fail(fetchStartErrorMessage(IOUtils.readURL(huc)));
        }
        return stripCookie(headerField);
    }

    public static String login(final PrivateKey pk, final X509Certificate ce) throws NoSuchAlgorithmException, KeyManagementException, IOException, MalformedURLException {

        HttpURLConnection connection = (HttpURLConnection) new URL("https://" + getSecureServerName() + "/login").openConnection();
        authenticateClientCert(pk, ce, connection);
        if (connection.getResponseCode() == 302) {
            assertEquals("https://" + getSecureServerName() + "/", connection.getHeaderField("Location").replaceFirst(":443$", ""));
            return stripCookie(connection.getHeaderField("Set-Cookie"));
        } else {
            return null;
        }
    }

    public static void authenticateClientCert(final PrivateKey pk, final X509Certificate ce, HttpURLConnection connection) throws NoSuchAlgorithmException, KeyManagementException {
        KeyManager km = new X509KeyManager() {

            @Override
            public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
                return "client";
            }

            @Override
            public String chooseServerAlias(String arg0, Principal[] arg1, Socket arg2) {
                return null;
            }

            @Override
            public X509Certificate[] getCertificateChain(String arg0) {
                return new X509Certificate[] {
                        ce
                };
            }

            @Override
            public String[] getClientAliases(String arg0, Principal[] arg1) {
                return new String[] {
                        "client"
                };
            }

            @Override
            public PrivateKey getPrivateKey(String arg0) {
                if (arg0.equals("client")) {
                    return pk;
                }
                return null;
            }

            @Override
            public String[] getServerAliases(String arg0, Principal[] arg1) {
                return new String[] {
                        "client"
                };
            }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(new KeyManager[] {
                km
        }, null, null);
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
        }
    }

    public static String getCSRF(URLConnection u) throws IOException {
        return getCSRF(u, 0);
    }

    public static String getCSRF(URLConnection u, int formIndex) throws IOException {
        String content = IOUtils.readURL(u);
        return getCSRF(formIndex, content);
    }

    public static String getCSRF(int formIndex, String content) throws Error {
        Pattern p = Pattern.compile("<input type='hidden' name='csrf' value='([^']+)'>");
        Matcher m = p.matcher(content);
        for (int i = 0; i < formIndex + 1; i++) {
            if ( !m.find()) {
                throw new Error("No CSRF Token:\n" + content);
            }
        }
        return m.group(1);
    }

    public static String executeBasicWebInteraction(String cookie, String path, String query) throws MalformedURLException, UnsupportedEncodingException, IOException {
        return executeBasicWebInteraction(cookie, path, query, 0);
    }

    public static String executeBasicWebInteraction(String cookie, String path, String query, int formIndex) throws IOException, MalformedURLException, UnsupportedEncodingException {
        HttpURLConnection uc = post(cookie, path, query, formIndex);
        if (uc.getResponseCode() == 302) {
            return null;
        }
        String error = fetchStartErrorMessage(IOUtils.readURL(uc));
        return error;
    }

    public static HttpURLConnection post(String cookie, String path, String query, int formIndex) throws IOException, MalformedURLException, UnsupportedEncodingException {
        String server = getServerName();
        if (loginCertificate != null) {
            server = getSecureServerName();
        }
        URLConnection uc = new URL("https://" + server + path).openConnection();
        authenticate((HttpURLConnection) uc, cookie);
        String csrf = getCSRF(uc, formIndex);

        uc = new URL("https://" + server + path).openConnection();
        authenticate((HttpURLConnection) uc, cookie);
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        os.write(("csrf=" + URLEncoder.encode(csrf, "UTF-8") + "&" //
                + query//
        ).getBytes("UTF-8"));
        os.flush();
        return (HttpURLConnection) uc;
    }

    public static HttpURLConnection get(String cookie, String path) throws IOException {
        String server = getServerName();
        if (loginCertificate != null) {
            server = getSecureServerName();
        }
        URLConnection uc = new URL("https://" + server + path).openConnection();
        authenticate((HttpURLConnection) uc, cookie);
        return (HttpURLConnection) uc;
    }

    public EmailAddress createVerifiedEmail(User u) throws InterruptedException, GigiApiException {
        return createVerifiedEmail(u, createUniqueName() + "test@test.tld");
    }

    public EmailAddress createVerifiedEmail(User u, String email) throws InterruptedException, GigiApiException {
        EmailAddress addr = new EmailAddress(u, email, Locale.ENGLISH);
        TestMail testMail = getMailReceiver().receive(addr.getAddress());
        String hash = testMail.extractLink().substring(testMail.extractLink().lastIndexOf('=') + 1);
        addr.verify(hash);
        getMailReceiver().assertEmpty();
        return addr;
    }

    public static URLConnection cookie(URLConnection openConnection, String cookie) {
        openConnection.setRequestProperty("Cookie", cookie);
        return openConnection;
    }

    private static User supporter;

    public static User getSupporter() throws GigiApiException, IOException {
        if (supporter != null) {
            return supporter;
        }
        int i = createVerifiedUser("fn", "ln", createUniqueName() + "@email.com", TEST_PASSWORD);
        try (GigiPreparedStatement ps = new GigiPreparedStatement("INSERT INTO `user_groups` SET `user`=?, `permission`=?::`userGroup`, `grantedby`=?")) {
            ps.setInt(1, i);
            ps.setString(2, Group.SUPPORTER.getDBName());
            ps.setInt(3, i);
            ps.execute();
        }
        clearCaches();
        supporter = User.getById(i);
        return supporter;
    }

    protected static void authenticate(HttpURLConnection uc, String cookie) throws IOException {
        uc.addRequestProperty("Cookie", cookie);
        if (loginCertificate != null) {
            try {
                authenticateClientCert(loginPrivateKey, loginCertificate.cert(), uc);
            } catch (GeneralSecurityException | GigiApiException e) {
                throw new IOException(e);
            }
        }
    }

    protected String cookieWithCertificateLogin(User u) throws IOException, GigiApiException {

        try {
            KeyPair kp;
            kp = generateKeypair();

            String csr;
            csr = generatePEMCSR(kp, "CN=" + u.getPreferredName().toString());

            Certificate c = new Certificate(u, u, Certificate.buildDN("CN", u.getPreferredName().toString()), Digest.SHA256, csr, CSRType.CSR, getClientProfile());
            final PrivateKey pk = kp.getPrivate();
            await(c.issue(null, "2y", u));
            final X509Certificate ce = c.cert();
            c.setLoginEnabled(true);
            loginCertificate = c;
            loginPrivateKey = pk;
            return login(pk, ce);
        } catch (InterruptedException e) {
            throw new GigiApiException(e.toString());
        } catch (GeneralSecurityException e) {
            throw new GigiApiException(e.toString());
        }

    }
}
