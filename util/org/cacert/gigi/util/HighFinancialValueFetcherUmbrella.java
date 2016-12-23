package org.cacert.gigi.util;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;

public class HighFinancialValueFetcherUmbrella extends HighFinancialValueFetcher {

    public HighFinancialValueFetcherUmbrella(File f, int max) {
        super(f, max, "https://s3-us-west-1.amazonaws.com/umbrella-static/top-1m.csv.zip");
    }

    private HashSet<String> printed = new HashSet<>();

    @Override
    public void handle(String line, PrintWriter fos) {
        String[] parts = line.split(",");
        // Assert that the value before the "," is an integer
        Integer.parseInt(parts[0]);

        String registrablePart = PublicSuffixes.getInstance().getRegistrablePart(parts[1]);
        if (registrablePart != null && printed.add(registrablePart)) {
            emit(fos, registrablePart);
            System.out.println(registrablePart);
        }

    }

}
