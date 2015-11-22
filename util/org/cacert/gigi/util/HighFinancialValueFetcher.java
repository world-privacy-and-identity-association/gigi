package org.cacert.gigi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HighFinancialValueFetcher {

    public static void main(String[] args) throws IOException {
        int max = 1000;
        if (args.length > 1) {
            max = Integer.parseInt(args[1]);
        }
        PrintWriter fos = new PrintWriter(new File(args[0]), "UTF-8");
        ZipInputStream zis = new ZipInputStream(new URL("https://s3.amazonaws.com/alexa-static/top-1m.csv.zip").openStream());
        ZipEntry ze;
        outer:
        while ((ze = zis.getNextEntry()) != null) {
            System.out.println(ze.getName());
            BufferedReader br = new BufferedReader(new InputStreamReader(zis, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int i = Integer.parseInt(parts[0]);
                if (i > max) {
                    zis.close();
                    break outer;
                }
                fos.println(parts[1]);
                System.out.println(line);
            }
        }
        fos.close();
    }

}
