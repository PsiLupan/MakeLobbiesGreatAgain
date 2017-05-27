package mlga.io.peer;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

import mlga.io.DirectoryWatcher;
import mlga.io.FileUtil;

/**
 * Incredibly grimy, unfinished (!) test class for parsing MLGA log files for (background) pairing of UID:IP to enable persistant blocks past dynamic IP ranges.
 * @author ShadowMoose
 *
 */
public class PeerTracker {
	private File dir = new File(new File(System.getenv("APPDATA")).getParentFile().getAbsolutePath()+"/Local/DeadByDaylight/Saved/Logs/");
	private File peerFile = new File(FileUtil.getMlgaPath()+"peers.json");
	private ArrayList<IOPeer> users = new ArrayList<IOPeer>();
	private String uid = null;
	private boolean active = false;


	public static void main(String[] args){
		new PeerTracker().start();
		try{
			// Simulate a delay for testing purposes, because this class runs as a threaded daemon.
			Thread.sleep(2000);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public PeerTracker(){
		// PeerSavers create emergency backups, so loop to check primary file, then attempt fallback if needed.
		for(int i=0; i<2; i++){
			try {
				PeerReader ps = new PeerReader(FileUtil.getSaveName(peerFile, i));
				while(ps.hasNext())
					users.add(ps.next());
				System.out.println("Loaded "+users.size()+" tracked users!");
				break;
			} catch (IOException e) {
				if(i==0){
					System.err.println("No Peers file located! Checking backups!");
				}
			}
		}
	}
	
	/** Launches this listener thread, in order to automatically update Peers. */
	public void start(){
		this.checkLogs();
		new DirectoryWatcher(dir){
			public void handle(File f, Event e){
				if(e == Event.DELETE)
					return;
				if(!f.getName().endsWith(".log"))
					return;
				processLog(f);
			}
		};
	}

	/**
	 * Run through all log files, checking for logged connections.
	 */
	public void checkLogs(){
		for(File f : dir.listFiles()){
			if(f.isDirectory())
				continue;
			if(!f.getName().endsWith(".log"))
				continue;
			System.out.println(f.getName());
			processLog(f);
		}
		System.out.println("Identified "+users.size()+" unique user/ip combos!");
		active = false;
		try {
			PeerSaver ps = new PeerSaver(this.peerFile);
			if(!ps.save(users)){
				System.err.println("Error saving Peers file!");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/* 
	 * If the Peer.matches() works, but the Peer is missing its ID, set the ID and save.
	 * Otherwise, if the Peer ID is set & matches, and the IP isn't saved in its list, add the IP and save.
	 * If none of the IOPeers trigger a match, build new IOPeer object, set name/IP, and save.
	 * 	++There really *should* be a matching peer for the IP by this point, as the peer should've been created for the IP when first joined to.
	 */
	private void processLog(File f){
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
							boolean matched = false;
							for(IOPeer iop : users){
								if(iop.matches(uid) || iop.matches(ip)){
									//System.out.println("\t+Found preexisting peer information. "+uid+" :: "+ip);
									if(!iop.hasUID()){
										System.out.println("\tDiscovered IP ["+ip+"] is UID: ["+uid+"]!");
										iop.setUID(uid);
									}
									if(!iop.hasIP(ip)){
										System.out.println("\tUser "+uid+" is @ new IP: "+ip);
										iop.addIP(ip);
									}
									matched = true;
								}
							}
							if(!matched){
								users.add(p);
								System.out.println("\tNew Peer: "+uid+" = "+ip);
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
