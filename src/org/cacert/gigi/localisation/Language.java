package org.cacert.gigi.localisation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Language {

    public static final String SESSION_ATTRIB_NAME = "lang";

    private static Locale[] supportedLocales;

    static {
        LinkedList<Locale> supported = new LinkedList<>();
        File locales = new File("locale");
        for (File f : locales.listFiles()) {
            if ( !f.getName().endsWith(".xml")) {
                continue;
            }
            String language = f.getName().split("\\.", 2)[0];
            supported.add(getLocaleFromString(language));
        }
        Collections.sort(supported, new Comparator<Locale>() {

            @Override
            public int compare(Locale o1, Locale o2) {
                return o1.toString().compareTo(o2.toString());
            }

        });
        supportedLocales = supported.toArray(new Locale[supported.size()]);
    }

    public static Locale getLocaleFromString(String language) {
        if (language.contains("_")) {
            String[] parts = language.split("_", 2);
            return new Locale(parts[0], parts[1]);

        } else {
            return new Locale(language);
        }
    }

    public static Locale[] getSupportedLocales() {
        return supportedLocales;
    }

    private static HashMap<String, Language> langs = new HashMap<String, Language>();

    private HashMap<String, String> translations = new HashMap<String, String>();

    private Locale l;

    private static Locale project(Locale l) {
        if (l == null) {
            return Locale.getDefault();
        }
        File file = new File("locale", l.toString() + ".xml");
        if ( !file.exists()) {
            return new Locale(l.getLanguage());
        }
        return l;
    }

    protected Language(Locale loc) throws ParserConfigurationException, IOException, SAXException {
        File file = new File("locale", loc.toString() + ".xml");
        l = loc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d = db.parse(new FileInputStream(file));
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

    public static Language getInstance(Locale language) {
        language = project(language);
        File file = new File("locale", language.toString() + ".xml");
        if ( !file.exists()) {
            return null;
        }
        Language l = langs.get(language.toString());
        if (l == null) {
            try {
                l = new Language(language);
                langs.put(language.toString(), l);
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
