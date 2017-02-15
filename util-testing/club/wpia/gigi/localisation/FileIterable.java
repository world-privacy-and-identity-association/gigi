package club.wpia.gigi.localisation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

public class FileIterable implements Iterable<String> {

    File f;

    public FileIterable(File f) {
        this.f = f;
    }

    @Override
    public Iterator<String> iterator() {
        try {
            return new Iterator<String>() {

                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));

                String s = null;

                private void getNext() {
                    if (s == null) {
                        try {
                            s = br.readLine();
                        } catch (IOException e) {
                            throw new IOError(e);
                        }
                    }
                }

                @Override
                public boolean hasNext() {
                    getNext();
                    return s != null;
                }

                @Override
                public String next() {
                    String out = s;
                    s = null;
                    return out;
                }
            };
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
