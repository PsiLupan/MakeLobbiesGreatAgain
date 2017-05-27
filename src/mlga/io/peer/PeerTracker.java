package mlga.io.peer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Incredibly grimy, unfinished (!) test class for parsing MLGA log files for (background) pairing of UID:IP to enable persistant blocks past dynamic IP ranges.
 * @author ShadowMoose
 *
 */
public class PeerTracker {
	static File dir = new File(new File(System.getenv("APPDATA")).getParentFile().getAbsolutePath()+"/Local/DeadByDaylight/Saved/Logs/");
	static ArrayList<IOPeer> users = new ArrayList<IOPeer>();
	static String uid = null;
	static boolean active = false;
	
	public static void main(String[] args){
		for(File f : dir.listFiles()){
			if(f.isDirectory())
				continue;
			if(!f.getName().endsWith(".log"))
				continue;
			System.out.println(f.getName());
			processLog(f);
		}
		System.out.println("Identified "+users.size()+" unique user/ip combos!");
		try {
			// Save example output list.
			PeerList.savePeers(new FileOutputStream("peers.json"), users);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/* TODO: On each completed name:ip pairing, run through all IOPeer object saved...
	 * If the Peer.matches() works, but the Peer is missing its ID, set the ID and save.
	 * Otherwise, if the Peer ID is set & matches, and the IP isn't saved in its list, add the IP and save.
	 * If none of the IOPeers trigger a match, build new IOPeer object, set name/IP, and save.
	 * 	++There really *should* be a matching peer for the IP by this point, as the peer should've been created for the IP when first joined to.
	 */
	private static void processLog(File f){
		try (Stream<String> stream = Files.lines(Paths.get(f.getPath()))) {			
			
			stream.forEachOrdered((l)->{				
				l = l.trim().toLowerCase();

				if(l.contains("connectionactive: 1"))
					active = true;
				if(l.contains("connectionactive: 0"))
					active = false;
				if(!active){
					uid = null;
					return;
				}
				if(l.contains("steam: - id:")){
					uid = l.split("id:")[1].split("\\[")[1].split("\\]")[0].trim();
				}
				if(l.contains("-- ipaddress:")){
					String ip = "";
					if(uid != null && active){
						ip = l.split("address:")[1].trim();
						if(ip.contains(":"))ip = ip.substring(0, ip.indexOf(":"));
						InetAddress ina = null;
						try {
							ina = InetAddress.getByName(ip);
							if(ina != null && ina.isAnyLocalAddress()){
								uid = null;
								active = false;
								return;
							}
							IOPeer p = new IOPeer();
							p.setUID(uid);
							p.addIP(ip);
							if(!users.contains(p)){
								users.add(p);
								System.out.println("\t"+uid+" = "+ip);
							}
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}finally{
							active = false;
							uid = null;
						}
					}

				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
