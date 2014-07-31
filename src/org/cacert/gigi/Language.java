package org.cacert.gigi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Language {

    private static HashMap<String, Language> langs = new HashMap<String, Language>();

    HashMap<String, String> translations = new HashMap<String, String>();

    Locale l;

    private Language(String language) throws ParserConfigurationException, IOException, SAXException {
        if (language.contains("_")) {
            String[] parts = language.split("_");
            l = new Locale(parts[0], parts[1]);
        } else {
            l = new Locale(language);
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(new FileInputStream(new File("locale", language + ".xml")));
        NodeList nl = d.getDocumentElement().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if ( !(nl.item(i) instanceof Element)) {
                continue;
            }
            Element e = (Element) nl.item(i);
            Element id = (Element) e.getElementsByTagName("id").item(0);
            Element msg = (Element) e.getElementsByTagName("msg").item(0);
            translations.put(id.getTextContent(), msg.getTextContent());
        }
        System.out.println(translations.size() + " strings loaded.");
    }

    public String getTranslation(String text) {
        String string = translations.get(text);
        if (string == null || string.equals("")) {
            return text;
        }
        return string;
    }

    public static Language getInstance(String language) {
        Language l = langs.get(language);
        if (l == null) {
            try {
                l = new Language(language);
                langs.put(language, l);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
        return l;
    }

    public Locale getLocale() {
        return l;
    }

}
