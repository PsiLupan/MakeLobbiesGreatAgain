package mlga;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.pcap4j.core.Pcaps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mlga.ui.MarkdownPanel;

public class Sanity {
	private final static Double version = 1.30;
	private static boolean headless = false;

	public static boolean check(){
		boolean[] checks = {checkGraphics(), checkUpdate(), checkJava(), checkPCap()};

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
			System.out.println("Pcap Info: " + Pcaps.libVersion());
		}catch(Error e){
			e.printStackTrace();
			message("You MUST have NPCap or WinPCap installed to allow this program to monitor the lobby!"
					+(Desktop.isDesktopSupported()?"\nAn installer link will attempt to open...":"Please go to https://www.winpcap.org/ and install it."));
			if(Desktop.isDesktopSupported()){
				try {
					Desktop.getDesktop().browse(new URL("https://nmap.org/npcap/").toURI());
				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
					message("We couldn't open the URL for you, so go to https://nmap.org/npcap/ and install it!");
				}
			}
			return false;
		}
		return true;
	}

	public static boolean checkUpdate(){
		try {
			URL update = new URL("https://api.github.com/repos/PsiLupan/MakeLobbiesGreatAgain/releases/latest");
			BufferedReader buf = new BufferedReader(new InputStreamReader(update.openStream()));
			//Gson gson = new Gson();
			JsonElement ele = new JsonParser().parse(buf);
			JsonObject obj = ele.getAsJsonObject();
			double newVersion = Double.parseDouble(obj.get("tag_name").getAsString().trim());
			if(version < newVersion){
				String html = obj.get("body").getAsString().trim();
				MarkdownPanel mp = new MarkdownPanel(newVersion, obj.get("name").getAsString(),  html);
				// Prompts the user with the update log, and waits for it to close.
				// If the user clicks a link, the JVM will terminate here.
				mp.prompt();
			}else{
				System.out.println("Version "+version+" :: Up to date!");
			}
		} catch (IOException | NumberFormatException nfe){
			nfe.printStackTrace();
			message("Unable to determine latest version. \nPlease manually check for an update!");
		}
		return true;
	}

	private static void message(String out){
		System.err.println(out);
		if(!headless){
			JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
