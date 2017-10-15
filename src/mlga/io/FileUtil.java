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
	 * Attempts to back up any copies of valid Files passed to it. <br>
	 * Supports creating multiple rolling backups of the same file within the same supplied backup dir.
	 *
	 * @param f                The file to duplicate
	 * @param backup_dir       The directory to store the backup in.
	 * @param max_extra_copies The number of copies beyond the base copy desired.
	 *
	 * @return True if the save works.
	 */
	public static boolean saveFile(File f, File backup_dir, int max_extra_copies) {
		if (!backup_dir.exists()) {
			backup_dir.mkdirs();
			System.out.println("Built backup directory: " + backup_dir.getAbsolutePath());
		}

		if (!f.exists()) {
			return false;
		}

		System.out.println("Saving: " + f);
		File copy = new File(backup_dir.getAbsolutePath() + "/" + f.getName());
		if (!copy.getParentFile().exists())
			copy.getParentFile().mkdirs();

		System.out.println(copy);
		if (copy.exists()) {
			//A copy of this file already exists
			if (f.length() < 12000 && (copy.length() - f.length()) > 10000) { //Verify the new file isn't corrupted, such as a massive filesize difference
				JOptionPane.showMessageDialog(null, "WARNING: Possible loss of progress. Backup was not created.", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			//Shuffle through existing backups, increment them all, and remove the oldest backup copy.
			for (int i = max_extra_copies; i > 0; i--) {
				File max = getSaveName(copy, i);
				//System.out.println(max);
				if (max.exists()) {
					if (i < max_extra_copies) {//Increment version.
						max.renameTo(getSaveName(copy, (i + 1)));
					} else {
						max.delete();//Delete oldest allowed copy.
					}
				}
			}
			//Finally, we increment the existing copy to '1'.
			copy.renameTo(getSaveName(copy, 1));
		}
		try {
			if (f.exists()) {
				Files.copy(f.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
				System.out.println("\t+Made backup of file!");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Simplified version of {@link #saveFile(File, File, int)},
	 * this method always uses the directory provided by {@link #getMlgaPath()},
	 * and appends the supplied string to the directory as a subdirectory path for the backup files.
	 *
	 * @param f                The file to duplicate
	 * @param subdirs          The subdirectory path within the MLGA directory to use for the copies.
	 * @param max_extra_copies The number of copies beyond the base copy desired.
	 *
	 * @return True if the save works.
	 */
	public static boolean saveFile(File f, String subdirs, int max_extra_copies) {
		return saveFile(f, new File(getMlgaPath() + subdirs + "/"), max_extra_copies);
	}

	/**
	 * Get a File object representing version <i>version</i> of the given File <i>f</i>.<br>
	 * It is crucial (for ease of tracking) that all backup files follow the same naming conventions.<br>
	 * This function exists to enforce those conventions.
	 */
	public static File getSaveName(File f, int version) {
		return new File(f.getParentFile().getAbsolutePath() + "/" + (version != 0 ? version + " - " : "") + f.getName());
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
