import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.io.FileOutputStream;
import java.io.File;

/**  The Settings class exposes access to rapidly read/save values for a key->value system.
	All settings keys/values are stored and read as Strings (and then potentially converted, depending on the method called),
		so be sure the stored objects convert.
	Uses an INI format currently, with newline stripping, so be sure and check what you're storing.
	INI format is less than ideal if we somehow ever needed to store multi-line, but for the existing data we'd rationally need,
		this is best for easy editing/validation in bug reports.
 */
public class Settings{
	private final static File save = new File("mlga.settings.ini");
	private static HashMap<String, String> loaded = new HashMap<String, String>();

	/** Loads in the saved settings, if possible. */
	public static void init(){
		if(!save.exists()){
			System.err.println("Save file has not been created yet. Skipping loading.");
			return;
		}
		try{
			BufferedReader bufferedReader = new BufferedReader(new FileReader(save));
			String line="";
			while ((line = bufferedReader.readLine()) != null){
				if(line.trim().startsWith(";") || !line.contains("=")){
					continue;//skip comments. It's INI, but we'll still pretend to follow conventions.
				}
				loaded.put(line.split("=")[0].trim(), line.substring(line.indexOf('=')+1).trim() );
			}
			bufferedReader.close();
		}catch(IOException e){
			e.printStackTrace();
			System.err.println("No settings were able to be loaded in.");
		}
	}

	/** Updates the setting for all future get calls, and saves the new settings file to persist. 
		value will be converted into a string.*/
	public static void set(String key, Object value){
		loaded.remove(key);
		loaded.put(key, value.toString());
		FileOutputStream o;
		try {
			o = new FileOutputStream(save);
			for(String k : loaded.keySet() ){
				o.write((k.replace("\n","").trim()+"="+(String)loaded.get(k).trim()+"\r\n").getBytes() );
			}
			o.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Get the value of the given key, or return <i>def</i> by default if it's not a saved value. */
	public static String get(String key, String def){
		if(!loaded.containsKey(key)){
			return def;
		}
		return loaded.get(key).trim();
	}

	/** See #get(). Returns the value of the key, converted to a double. Mostly just here for convenient casting.*/
	public static double getDouble(String key, double def){
		return Double.parseDouble(get(key, ""+def));
	}
}
