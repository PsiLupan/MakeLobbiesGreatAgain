import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JOptionPane;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Geolocate implements Runnable {
	private String ip;

	Geolocate(String ip){
		this.ip = ip;
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

		System.out.println("Attempting connection to a killer");
		if(code != null){
			System.out.println("Locale: " + code);
			System.out.println();
			
			if(!code.equals("US") && !code.equals("CA") && !code.equals("MX")){ //TODO: Provide user selection
				JOptionPane.showMessageDialog(null,
						"Killer is located in "+ code +".",
						"Location Warning",
						JOptionPane.WARNING_MESSAGE);
			}
		}else{
			System.out.println("Locale lookup failed");
		}
	}
}