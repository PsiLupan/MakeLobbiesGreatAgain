package mlga.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.JOptionPane;

/**
 * Utility class for MLGA, includes various methods commonly needed by various modules of the program.
 *
 * @author ShadowMoose
 */
public class FileUtil {

	/**
	 * Get the base directory path for all MLGA files, if left at default .  <br>
	 * Can be changed by the user, by editing 'base_dir' in the settings ini. <br>
	 * The path given will always end with a backslash. <br>
	 *
	 * @return A String file path, for convenience.
	 */
	public static String getMlgaPath() {
		String f = Settings.get("base_dir", null);
		if (f == null) {
			f = new File(System.getenv("APPDATA") + "/MLGA/").getAbsolutePath();
			Settings.set("base_dir", f);
		}
		return (f.endsWith("/") ? f : (f + "/"));
	}

	/**
	 * Generate an InputStream to the given resource file name.  <br>
	 * Automatically toggles between JAR and Build paths.
	 *
	 * @param resourceName The name or relative filepath (if resource is deeper than just "resources/") of the desired File.
	 *
	 * @return Null if File cannot be found, otherwise the resource's Stream.
	 */
	public static InputStream localResource(String resourceName) {
		if (ClassLoader.getSystemClassLoader().getResourceAsStream("resources/" + resourceName) != null) {
			return ClassLoader.getSystemClassLoader().getResourceAsStream("resources/" + resourceName);
		} else {
			return ClassLoader.getSystemClassLoader().getResourceAsStream("src/resources/" + resourceName);
		}
	}

}
