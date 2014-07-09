package org.cacert.gigi.ping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class HTTPFetch extends DomainPinger {

	@Override
	public void ping(String domain, String configuration, String expToken) {
		try {
			URL u = new URL("http://" + domain + "/cacert_rai.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream(), "UTF-8"));
			String line = br.readLine();
			if (line == null) {
				// empty
				return;
			}
			if (line.equals(expToken)) {
				// found
			}
			// differ
		} catch (IOException e) {
			e.printStackTrace();
			// error
		}
	}
}
