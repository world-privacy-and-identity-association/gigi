package club.wpia.gigi.util;

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
        try {
            if (Integer.parseInt(parts[0]) < 1) {
                throw new NumberFormatException("We expect a number greater then zero for the first column.");
            }
        } catch (NumberFormatException nfe) {
            // Bail on lines with invalid first field
            throw new Error("Invalid format of first column.", nfe);
        }

        String registrablePart = PublicSuffixes.getInstance().getRegistrablePart(parts[1]);
        if (registrablePart != null && printed.add(registrablePart)) {
            emit(fos, registrablePart);
            System.out.println(registrablePart);
        }

    }

}
