package org.cacert.gigi.util;

import java.io.File;
import java.io.PrintWriter;

public class HighFinancialValueFetcherAlexa extends HighFinancialValueFetcher {

    public HighFinancialValueFetcherAlexa(File f, int max) {
        super(f, max, "https://s3.amazonaws.com/alexa-static/top-1m.csv.zip");
    }

    @Override
    public void handle(String line, PrintWriter fos) {
        String[] parts = line.split(",");
        // Assert that the value before the "," is an integer
        Integer.parseInt(parts[0]);

        emit(fos, parts[1]);
        System.out.println(parts[1]);
    }

}
