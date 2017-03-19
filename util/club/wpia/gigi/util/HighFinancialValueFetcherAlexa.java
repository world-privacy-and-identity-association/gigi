package club.wpia.gigi.util;

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
        try {
            if (Integer.parseInt(parts[0]) < 1) {
                throw new NumberFormatException("We expect a number greater then zero for the first column.");
            }
        } catch (NumberFormatException nfe) {
            // Bail on lines with invalid first field
            throw new Error("Invalid format of first column.", nfe);
        }

        emit(fos, parts[1]);
        System.out.println(parts[1]);
    }

}
