package club.wpia.gigi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class HighFinancialValueFetcher {

    public final int max;

    private File f;

    private String base;

    public HighFinancialValueFetcher(File f, int max, String base) {
        this.f = f;
        this.max = max;
        this.base = base;
    }

    public static void main(String[] args) throws IOException {
        int max = 1000;
        if (args.length > 1) {
            max = Integer.parseInt(args[1]);
        }
        HighFinancialValueFetcher fetcher;
        if (args.length > 2 && "--alexa".equals(args[2])) {
            fetcher = new HighFinancialValueFetcherAlexa(new File(args[0]), max);
        } else {
            fetcher = new HighFinancialValueFetcherUmbrella(new File(args[0]), max);
        }
        fetcher.fetch();
    }

    public final void fetch() throws IOException {
        try (PrintWriter fos = new PrintWriter(f, "UTF-8"); ZipInputStream zis = new ZipInputStream(new URL(base).openStream())) {
            ZipEntry ze;
            outer:
            while ((ze = zis.getNextEntry()) != null) {
                System.out.println(ze.getName());
                BufferedReader br = new BufferedReader(new InputStreamReader(zis, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    handle(line, fos);
                    if (entries == -1) {
                        break outer;
                    }
                }
            }
        }
    }

    private int entries;

    public void emit(PrintWriter fos, String value) {
        fos.println(value);
        if (entries == -1 || entries++ > max) {
            entries = -1;
        }
    }

    public abstract void handle(String line, PrintWriter fos);
}
