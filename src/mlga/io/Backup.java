package mlga.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Backup is an all-in-one class built to ask for permission, monitor, and then save backup copies of the user's saves. <br>
 * To do this, it attempts first to automatically locate the user's Steam installation by reading the registry. 
 * If this fails, it will prompt for them to manually select their installation.  <br>
 * The process runs in the background as a low-cost daemon, and will automatically terminate when the Main program does.
 * @author ShadowMoose
 */
public class Backup {
	private static File backup_dir = new File(System.getenv("APPDATA")+"/MLGA/backup/");
	/** Maximum copies of each file backup we'd like to store, <b>not including</b> the most recent backup. */
	private static int max_extra_copies = 2;

	/** Performs a prompt (if first-time launch), or otherwise checks with Settings to verify if we're allowed to auto-backup. */
	public static boolean enabled(){
		double pref = Settings.getDouble("backup_enabled", -1);
		System.out.println("Backup Pref: "+pref);
		if(pref==-1){
			boolean prompt = JOptionPane.showConfirmDialog(null, "Would you like MLGA to automatically back up your in-game saves, to help prevent losing them?", "Backup Preferences", 0)==0;
			Settings.set("backup_enabled", prompt?1:0);
			Settings.set("backup_dest", backup_dir.getAbsolutePath());
			Settings.set("backup_copies", max_extra_copies);
		}
		if(Settings.getDouble("backup_enabled", 0)==1){
			backup_dir = new File(Settings.get("backup_dest", backup_dir.getAbsolutePath()) );
			max_extra_copies = (int)Settings.getDouble("backup_copies", max_extra_copies);
			return true;
		}
		return false;
	}
	
	/** Launches the backup daemon. <br>
	 * Runs in background , monitoring the Steam directory for DbD file changes, then copies those changed files over to a safe backup location.
	 */
	public static void startBackupDaemon(){
		Path steam = getSteam();
		if(steam==null){
			System.err.println("Steam path is null. Cannot enable automatic backups.");
			JOptionPane.showMessageDialog(null, "Unable to locate valid Steam directory. Backups can not occur.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		new DirectoryWatcher(steam.toFile(), true){
			public void handle(File f, Event e){
				if(e==Event.DELETE)
					return;
				saveFile(f);
			}
			
			public boolean followDir(File dir){
				return dir.getAbsolutePath().contains("381210") && dir.getAbsolutePath().toLowerCase().contains("profilesaves");
			}
		};
	}

	/** Gets, either automatically or via prompt, the directory Steam's <b>userdata</b> Folder is installed in.<br>
	 * @return <i>null</i> if an invalid directory is found. 
	 */
	private static Path getSteam(){
		//If we had=ve the Steam Path location already stored in Settings, we skip the lookup.
		String p = Settings.get("steam_path", null);
		if(p!=null){
			return new File(p).toPath();
		}

		//Steam stores the path it lives at in the Windows Registry. So far as I can tell, this is the best approach to locating it.
		//This method of calling CMD to lookup a registry key is "hacky", but Windows compliant over most versions. Might need to actually use a library though.
		//There should be a fallback prompt to manually select the directory anyways, so it's not a huge deal if this approach fails occasionally on some systems.
		String path = readRegistry("HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam\\", "SteamPath");
		System.out.println("Steam path: ["+path+"]");
		
		File dir = null;
		
		//If auto-lookup fails, we prompt for the user to manually supply a path via file selector.
		//We then save this path to Settings, so the user can manually alter it (and so we never need to run the lookup again).
		if(path!=null){
			dir = new File(path+"/userdata/");
			if(!dir.exists()){
				System.err.println("Invalid Steam directory located!");
				path = null;
				dir = null;
			}
		}
		if(path==null){
			JOptionPane.showMessageDialog(null, "MLGA was unable to automatically locate your Steam installation folder, so please select it in the following prompt.", "Backup Location", JOptionPane.ERROR_MESSAGE);
			JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new java.io.File("."));
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int r = fc.showSaveDialog(null);
			if(r == JFileChooser.APPROVE_OPTION) {
				dir = fc.getSelectedFile();
			}else{
				return null;
			}
		}
		if(dir!=null){
			Settings.set("steam_path", dir.getAbsolutePath());
			return dir.toPath();
		}
		return null;
	}

	/** Attempts to back up any copies of valid saves it can find for game 381210, "Dead By Daylight". <br>
	 * This code will <b>absolutely not</b> work automatically on anything other than a Windows setup, but should work on most Win versions. <br>
	 * We'd want a prompt before this function runs, the first time the app runs, to see if the user wants us to back up their data.
	 * */
	private static boolean saveFile(File f){
		if(!backup_dir.exists()){
			backup_dir.mkdirs();
			System.out.println("Built backup directory: "+backup_dir.getAbsolutePath());
		}
		// Accept only (existing Files), (*.profj* files), and ignore *.stmp temp files.
		if(f.exists() && f.getAbsolutePath().contains("profj") && !f.getName().contains(".stmp")){ 
			// The getParent() chain below reaches up and grabs the profile ID for this save's user, so we can keep all users as separate backups.
			File copy = new File(backup_dir.getAbsolutePath()+"/"+f.getParentFile().getParentFile().getParentFile().getParentFile().getName()+"/"+f.getName());
			if(!copy.getParentFile().exists()){
				copy.getParentFile().mkdirs();
			}
			
			long last = 0L;
			if(copy.exists()){
				last = copy.lastModified();
			}
			if(f.lastModified()>last){
				if(copy.exists()){
					//A copy of this file already exists, so we must shuffle through existing backups, increment them all, and remove the oldest backup copy.
					for(int i=max_extra_copies; i>0;i--){
						File max = backupFile(copy, i);
						if(max.exists()){
							if(i<max_extra_copies){//Increment version.
								max.renameTo(backupFile(copy, (i+1)) );
							}else{
								max.delete();//Delete oldest allowed copy.
							}
						}
					}
					//Finally, we increment the existing copy to '1'.
					copy.renameTo(backupFile(copy, 1) );
				}
				try {
					Files.copy(f.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
					System.out.println("\t+Made backup of file!");
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}

		}
		return true;
	}

	/** Get a File object representing version <i>version</i> of the given File <i>f</i>.<br>
	 * It is crucial (for ease of tracking) that all backup files follow the same naming conventions.<br>
	 * This function exists to enforce those conventions. */
	private static File backupFile(File f, int version){
		return new File(f.getParentFile().getAbsolutePath()+"/"+version+" - "+f.getName());
	}

	/**
	 * 
	 * @param location path in the registry
	 * @param key registry key
	 * @return registry value or null if not found
	 */
	private static final String readRegistry(String location, String key){
		try {
			// Run reg query, then read output.
			Process process = Runtime.getRuntime().exec("reg query "+ location + " /v " + key);
			process.waitFor();
			BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String output="";
			String s;
			while((s = r.readLine())!=null){
				output+=s;
			}
			r.close();
			// Output has the following format:
			// \n<Version information>\n\n<key>\t<registry type>\t<value>
			if( !output.contains("    ") && !output.contains("\t")){
				return null;
			}

			// Parse out the value
			String[] parsed = output.split("\t|    ");
			return parsed[parsed.length-1].trim();
		}catch (Exception e) {
			return null;
		}
	}
}
