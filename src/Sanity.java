import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.pcap4j.core.Pcaps;

public class Sanity {
	private static boolean headless = false;

	public static boolean check(){
		boolean[] checks = {checkGraphics(), checkForUpdate(), checkJava(), checkPCap()};

		for(boolean e : checks){
			if(!e){
				return false;
			}
		}
		return true;
	}

	/** Check for a valid graphical environment. */
	private static boolean checkGraphics(){
		if(GraphicsEnvironment.isHeadless()){
			headless = true;
			message("This program requires a graphical environment to run!\nIt's weird that you even got this far.");
			return false;
		}
		return true;
	}

	/** Check the current MLGA Version. */
	private static boolean checkForUpdate(){
		String version = "";

		try (InputStream is = new URL("https://psilupan.github.io/MLGA/version.txt").openStream();
				BufferedReader buf = new BufferedReader(new InputStreamReader(is))) {
			version = buf.readLine();
			System.out.println("Latest Version: " + version);
		}catch(Exception e){
			e.printStackTrace();
		}

		if(!Boot.version.equals(version)){
			message("There's a new version of MLGA available."
					+(Desktop.isDesktopSupported()?"\nA link will attempt to open...":"\nPlease go to https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases/ and get it."));
			if(Desktop.isDesktopSupported()){
				try {
					Desktop.getDesktop().browse(new URL("https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases/tag/"+version).toURI());
				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
					message("We couldn't open the URL for you, please go to https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases/ and get it!");
				}
			}
			return false;
		}
		return true;
	}

	/** Check the current Java Version. */
	private static boolean checkJava(){
		String v = System.getProperty("java.version");
		System.out.println("Java Version: "+v);
		
		double version = Double.parseDouble(v.substring(0, v.indexOf('.', 2)));
		if(version < 1.8){
			message("Java version 1.8 or higher is required!\nYou are currently using "+version+"!\n");
			return false;
		}
		return true;
	}

	/** Check the WinPcap lib installation. */
	private static boolean checkPCap(){
		try{
			System.out.println("Pcap Info: "+Pcaps.libVersion());//TODO: Not actually positive this throws an error if the lib is gone... Needs testing.
		}catch(Exception e){
			e.printStackTrace();
			message("You MUST have WinPcap installed to allow this program to monitor the lobby!"
					+(Desktop.isDesktopSupported()?"\nAn installer link will attempt to open...":"\nPlease go to https://www.winpcap.org/ and install it."));
			if(Desktop.isDesktopSupported()){
				try {
					Desktop.getDesktop().browse(new URL("https://www.winpcap.org/").toURI());
				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
					message("We couldn't open the URL for you, so go to https://www.winpcap.org/ and install it!");
				}
			}
			return false;
		}
		return true;
	}

	private static void message(String out){
		if(headless){
			System.err.println(out);
			return;
		}
		JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
	}
}
