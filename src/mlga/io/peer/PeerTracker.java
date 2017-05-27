package mlga.io.peer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CopyOnWriteArrayList;

import mlga.io.DirectoryWatcher;
import mlga.io.FileUtil;

/**
 * Incredibly grimy, unfinished (!) test class for parsing MLGA log files for (background) pairing of UID:IP to enable persistant blocks past dynamic IP ranges.
 * @author ShadowMoose
 *
 */
public class PeerTracker {
	private File dbdLogDir = new File(new File(System.getenv("APPDATA")).getParentFile().getAbsolutePath()+"/Local/DeadByDaylight/Saved/Logs/");
	private static File peerFile = new File(FileUtil.getMlgaPath()+"peers.mlga");
	private static CopyOnWriteArrayList<IOPeer> peers = new CopyOnWriteArrayList<IOPeer>();
	private static boolean saving = false;
	private String uid = null;
	private boolean active = false;

	/**
	 * Creates a PeerTracker, which instantly loads the Peer List into memory.  <br>
	 * Calling {@link #start()} will launch the passive listening component, which
	 * will keep the Peer List updated as new logs are created.
	 */
	public PeerTracker(){
		// PeerSavers create emergency backups, so loop to check primary file, then attempt fallback if needed.
		for(int i = 0; i < 2; i++){
			try {
				PeerReader ps = new PeerReader(FileUtil.getSaveName(peerFile, i));
				while(ps.hasNext())
					peers.add(ps.next());
				System.out.println("Loaded "+peers.size()+" tracked users!");
				if(i != 0){
					// If we had to check a backup, re-save the backup as the primary instantly.
					savePeers();
				}
				break;
			} catch (IOException e) {
				//e.printStackTrace();
				if(i == 0){
					System.err.println("No Peers file located! Checking backups!");
				}
			}
		}
	}

	/** Launches this listener thread, in order to automatically update Peers. */
	public void start(){
		// Start off by updating from any existing logs that may not have been parsed yet.
		this.checkLogs();
		// Register to listen for, and process, new log files.
		new DirectoryWatcher(dbdLogDir){
			public void handle(File f, Event e){
				if(e == Event.DELETE)
					return;
				if(!f.getName().endsWith(".log"))
					return;
				processLog(f);
			}
		};

		// TODO: Adding a listener to each Peer, or a clever callback, might be better.
		//    + Though, this method does cut down on file writes during times of many updates.
		Thread t = new Thread("IOPeerSaver"){
			public void run(){
				while(true){
					for(IOPeer p : peers){
						if(!p.saved){
							try{
								// Intentionally hang if we located a Peer to save, to allow any other Peers to batch updates together.
								Thread.sleep(10);
							}catch(Exception e){e.printStackTrace();}

							savePeers();
							// Exit for() loop and re-check list.
							break;
						}
					}
					// Wait 100ms before rechecking Peers for changes.
					try{Thread.sleep(100);}catch(Exception e){}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Run through all log files, checking for new Peers.
	 */
	public void checkLogs(){
		for(File f : dbdLogDir.listFiles()){
			if(f.isDirectory())
				continue;
			if(!f.getName().endsWith(".log"))
				continue;
			System.out.println(f.getName());
			processLog(f);
		}
		System.out.println("Identified "+peers.size()+" unique user/ip combos!");
		active = false;
	}

	/**
	 * Attempts to save the list of IOPeers.  <br>
	 * Should be called whenever a Peer's information is updated.  <br><br>
	 * Since the Peer List is static between all instances of PeerTracker, this method may be called by anything.
	 * @return True if this save works. May not work if a save is already underway.
	 */
	private boolean savePeers(){
		if(saving){
			// This type of check is less than ideal,
			// but if save is being called at the same time, the first instance should still save all listed IOPeers.
			System.err.println("Peer File is busy!");
			return false;
		}
		System.err.println("Saving peers!");
		// Flag that the save file is busy, to avoid thread shenanigans.
		saving = true;
		try {
			PeerSaver ps = new PeerSaver(peerFile);
			ps.save(peers);
			saving = false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		saving = false;
		return false;
	}

	/** The main method of interfacing with the Peer List, 
	 * this method either retrieves an existing IOPeer object which "owns" the given IP, 
	 * or it returns the new IOPeer object generated - and containing - the new IP.
	 * @param ip
	 */
	public IOPeer getPeer(String ip){
		for(IOPeer p : peers){
			if(p.hasIP(ip))
				return p;
		}
		System.out.println("New Peer located!");
		IOPeer p = new IOPeer();
		p.addIP(ip);
		peers.add(p);
		return p;
	}

	/** 
	 * Iterates through the Log file, pairing UIDs and IPs that it can find,
	 * and adding them to the IOPeer list or updating existing IOPeers where missing info is found.
	 * 
	 * @param f The file to process.
	 */
	private void processLog(File f){
		try (BufferedReader br = new BufferedReader(new FileReader(f))){
			String l;
			while((l = br.readLine()) != null){
				l = l.trim().toLowerCase();

				if(l.contains("connectionactive: 1"))
					active = true;
				if(l.contains("connectionactive: 0"))
					active = false;
				if(!active){
					uid = null;
					continue;
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
							if(ina == null || (ina != null && (ina.isAnyLocalAddress() || ina.isSiteLocalAddress()))){
								uid = null;
								active = false;
								continue;
							}
							IOPeer p = new IOPeer();
							p.setUID(uid);
							p.addIP(ip);
							boolean matched = false;
							for(IOPeer iop : peers){
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
								peers.add(p);
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
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
