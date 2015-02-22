package org.cacert.gigi.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class FetchLocales {

    public static final String DOWNLOAD_SERVER = "translations.cacert.org";

    public static final String PO_URL_TEMPLATE = "http://" + DOWNLOAD_SERVER + "/export/cacert/%/messages.po";

    public static final String[] AUTO_LANGS = new String[] {
            "en", "de", "nl", "pt_BR", "fr", "sv", "it", "es", "hu", "fi", "ja", "bg", "pt", "da", "pl", "zh_CN", "ru", "lv", "cs", "zh_TW", "el", "tr", "ar"
    };

    public static void main(String[] args) throws IOException, ParserConfigurationException, TransformerException {
        System.out.println("downloading locales ...");
        File locale = new File("locale");
        if ( !locale.mkdir()) {
            throw new IOException("Could not create locales directory.");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        for (String lang : AUTO_LANGS) {
            Document doc = db.newDocument();
            doc.appendChild(doc.createElement("translations"));
            URL fetch = new URL(PO_URL_TEMPLATE.replace("%", lang));
            URLConnection uc = fetch.openConnection();
            Scanner sc = new Scanner(new InputStreamReader(uc.getInputStream(), "UTF-8"));
            String s = readLine(sc);
            StringBuffer contents = new StringBuffer();
            String id = "";
            while (s != null) {
                if (s.startsWith("msgid")) {
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
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            DOMSource source = new DOMSource(doc);
            FileOutputStream fos = new FileOutputStream(new File(locale, lang + ".xml"));
            StreamResult result = new StreamResult(fos);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
            fos.close();
        }
        System.out.println("Done.");
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
