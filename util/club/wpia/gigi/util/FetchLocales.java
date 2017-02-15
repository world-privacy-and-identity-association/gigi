package club.wpia.gigi.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class FetchLocales {

    public static final String DOWNLOAD_SERVER = "pootle.cacert1.dogcraft.de";

    public static String PO_URL_TEMPLATE = "https://" + DOWNLOAD_SERVER + "/%/gigi/messages.po";

    public static final String[] AUTO_LANGS = new String[] {
            "de"
    };

    public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerException {
        if (args.length != 0) {
            PO_URL_TEMPLATE = args[0];
        }
        System.out.println("downloading locales ...");
        File locale = new File("locale");
        if ( !locale.isDirectory() && !locale.mkdir()) {
            throw new IOException("Could not create locales directory.");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        for (String lang : AUTO_LANGS) {
            Document doc = db.newDocument();
            doc.appendChild(doc.createElement("translations"));

            int count = addTranslationsFromPo(doc, new URL(PO_URL_TEMPLATE.replace("%", lang)));

            System.out.println("Strings for language " + lang + ": " + count);

            writeTranslationToFile(doc, new File(locale, lang + ".xml"));
        }
        Document doc = db.newDocument();
        doc.appendChild(doc.createElement("translations"));
        System.out.println("Creating empty en.xml");
        writeTranslationToFile(doc, new File(locale, "en.xml"));
        System.out.println("Done.");
    }

    private static void writeTranslationToFile(Document doc, File file) throws TransformerFactoryConfigurationError, TransformerConfigurationException, FileNotFoundException, TransformerException, IOException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

        DOMSource source = new DOMSource(doc);
        FileOutputStream fos = new FileOutputStream(file);
        StreamResult result = new StreamResult(fos);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
        fos.close();
    }

    private static int addTranslationsFromPo(Document doc, URL fetch) throws IOException, UnsupportedEncodingException {
        URLConnection uc = fetch.openConnection();
        Scanner sc = new Scanner(new InputStreamReader(uc.getInputStream(), "UTF-8"));
        String s = readLine(sc);
        StringBuffer contents = new StringBuffer();
        String id = "";
        int count = 0;
        while (s != null) {
            if (s.startsWith("msgid")) {
                count++;
                contents.delete(0, contents.length());
                s = readString(s, sc, contents);
                id = contents.toString();
                continue;
            } else if (s.startsWith("msgstr")) {
                contents.delete(0, contents.length());
                // System.out.println("msgstr");
                s = readString(s, sc, contents);
                String msg = contents.toString().replace("\\\"", "\"").replace("\\n", "\n");
                insertTranslation(doc, id, msg);
            } else if (s.startsWith("#")) {
                // System.out.println(s);
            } else if (s.equals("") || s.equals("\r")) {

            } else {
                System.out.println("unknown line: " + s);
            }
            s = readLine(sc);
        }
        return count;
    }

    private static String readLine(Scanner sc) {
        String line = sc.findWithinHorizon("[^\n]*\n", 0);
        if (line == null) {
            return null;
        }
        return line.substring(0, line.length() - 1);
    }

    private static void insertTranslation(Document doc, String id, String msg) {
        Node idN = doc.createTextNode(id);
        Node textN = doc.createTextNode(msg);
        Element tr = doc.createElement("translation");
        Element e = doc.createElement("id");
        e.appendChild(idN);
        tr.appendChild(e);
        e = doc.createElement("msg");
        e.appendChild(textN);
        tr.appendChild(e);
        doc.getDocumentElement().appendChild(tr);
    }

    private static String readString(String head, Scanner sc, StringBuffer contents) throws IOException {
        head = head.split(" ", 2)[1];
        contents.append(head.substring(1, head.length() - 1));
        String s;
        while ((s = readLine(sc)) != null) {
            if ( !s.startsWith("\"")) {
                break;
            }
            contents.append(s.substring(1, s.length() - 1));
        }
        return s;
    }

}
