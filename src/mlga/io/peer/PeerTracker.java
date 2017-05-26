package mlga.io.peer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Incredibly grimy, unfinished (!) test class for parsing MLGA log files for (background) pairing of UID:IP to enable persistant blocks past dynamic IP ranges.
 * @author ShadowMoose
 *
 */
public class PeerTracker {
	static File dir = new File(new File(System.getenv("APPDATA")).getParentFile().getAbsolutePath()+"/Local/DeadByDaylight/Saved/Logs/");
	static ArrayList<IOPeer> users = new ArrayList<IOPeer>();
	
	public static void main(String[] args){
		for(File f : dir.listFiles()){
			if(f.isDirectory())continue;
			if(!f.getName().endsWith(".log"))continue;
			System.out.println(f.getName());
			processLog(f);
		}
		System.out.println("Identified "+users.size()+" unique user/ip combos!");
	}
	/* TODO: On each completed name:ip pairing, run through all IOPeer object saved...
	 * If the Peer.matches() works, but the Peer is missing its ID, set the ID and save.
	 * Otherwise, if the Peer ID is set & matches, and the IP isn't saved in its list, add the IP and save.
	 * If none of the IOPeers trigger a match, build new IOPeer object, set name/IP, and save.
	 * 	++There really *should* be a matching peer for the IP by this point, as the peer should've been created for the IP when first joined to.
	*/
	private static void processLog(File f){
		
		try {
			BufferedReader log = new BufferedReader(new FileReader(f));
			String l;
			
			String uid = null;
			boolean active = false;
			
			while((l = log.readLine())!=null){
				l = l.trim().toLowerCase();
				
				if(l.contains("connectionactive: 1"))active = true;
				if(l.contains("connectionactive: 0"))active = false;
				if(!active){
					uid = null;
					continue;
				}
				if(l.contains("steam: - id:")){
					uid = l.split("id:")[1].split("\\[")[1].split("\\]")[0].trim();
				}
				if(l.contains("-- ipaddress:")){
					String ip = "";
					if(uid!=null && active){
						ip = l.split("address:")[1].trim();
						if(ip.contains(":"))ip = ip.substring(0, ip.indexOf(":"));
						InetAddress ina = InetAddress.getByName(ip);
						if(ina.isAnyLocalAddress()){
							uid = null;
							active = false;
							continue;
						}
						IOPeer p = new IOPeer();
						p.setUID(uid);
						p.addIP(ip);
						if(!users.contains(p)){
							users.add(p);
							System.out.println("\t"+uid+" = "+ip);
						}
						active = false;
						uid = null;
					}
					
				}
			}
			log.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
