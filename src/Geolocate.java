import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Geolocate implements Runnable {
	private String ip;
	private Overlay ui;
	
	Geolocate(String ip, Overlay ui){
		this.ip = ip;
		this.ui = ui;
	}

	@Override
	public void run() {
		try {
			geolocate();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	public void geolocate() throws IOException, ParseException{
		String code = null;
		URL url = new URL("http://freegeoip.net/json" + ip);

		try (InputStream is = url.openStream();
				BufferedReader buf = new BufferedReader(new InputStreamReader(is))) {
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(buf);
			code = (String)obj.get("country_code");
		}

		//System.out.println("Attempting connection to a killer");
		if(code != null){
			ui.setKillerLocale(code);
			//System.out.println("Locale: " + code);
			//System.out.println();
		}else{
			//System.out.println("Locale lookup failed");
		}
	}
}