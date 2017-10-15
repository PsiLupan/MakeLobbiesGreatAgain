package mlga.io.peer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import mlga.io.DirectoryWatcher;
import mlga.io.FileUtil;
import mlga.io.Preferences;
import mlga.io.peer.IOPeer.Status;
import mlga.io.peer.kindred.Kindred;

/**
 * Class for background parsing Dead by Daylight log files into pairing of UID:IP to enable persistant ratings past dynamic IP ranges.
 *
 * @author ShadowMoose
 */
public class PeerTracker implements Runnable {
	private File dbdLogDir = new File(new File(System.getenv("APPDATA")).getParentFile().getAbsolutePath() + "/Local/DeadByDaylight/Saved/Logs/");
	private static File peerFile = new File(FileUtil.getMlgaPath() + "peers.mlga");
	private static CopyOnWriteArrayList<IOPeer> peers = new CopyOnWriteArrayList<IOPeer>();
	private static boolean saving = false;
	private String uid = null;
	private boolean active = false;
	private final Kindred kindred;

	/**
	 * Creates a PeerTracker, which instantly loads the Peer List into memory.  <br>
	 * Calling {@link #start()} will launch the passive listening component, which
	 * will keep the Peer List updated as new logs are created.
	 */
	public PeerTracker() {
		//Initialize Kindred System.
		this.kindred = new Kindred();

		// PeerSavers create emergency backups, so loop to check primary file, then attempt fallback if needed.
		for (int i = 0; i < 2; i++) {
			try {
				PeerReader ps = new PeerReader(FileUtil.getSaveName(peerFile, i));
				while (ps.hasNext())
					peers.add(ps.next());
				System.out.println("Loaded " + peers.size() + " tracked users!");

				if (i != 0) // If we had to check a backup, re-save the backup as the primary instantly.
					savePeers();

				break;
			} catch (Exception e) {
				e.printStackTrace();
				if (i == 0)
					System.err.println("No Peers file located! Checking backups!");
			}
		}
	}

	/** Launches this listener thread, in order to automatically update Peers. */
	public void start() {
		// Initially check for any Legacy peer files.
		this.checkLegacy();
		// Start off by updating from any existing logs that may not have been parsed yet.
		this.checkLogs();
		// Register to listen for, and process, new log files.
		new DirectoryWatcher(dbdLogDir) {
			public void handle(File f, Event e) {
				if (e == Event.DELETE)
					return;
				if (e == Event.CREATE)
					return;
				if (!f.getName().endsWith(".log"))
					return;
				processLog(f, true);
			}
		};

		// Adding a listener to each Peer, or a clever callback, might be better.
		//    + Though, this method does cut down on file writes during times of many updates.
		Thread t = new Thread(this, "IOPeerSaver");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void run() {
		while (true) {
			if (peers.stream().anyMatch(p -> !p.saved)) {
				savePeers();
			}
			// Wait 100ms before rechecking Peers for changes.
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks for any Legacy peer files using the (now outdated) Preferences class.
	 */
	private void checkLegacy() {
		if (Preferences.prefsFile.exists()) {
			Preferences.init();
			// Build IOPeers using Legacy save.
			Preferences.prefs.keySet().forEach(key -> {
				IOPeer p = new IOPeer();
				p.addPrehashedIP(key);
				p.setStatus(Preferences.prefs.get(key) ? Status.BLOCKED : Status.LOVED);
				peers.add(p);
			});
			// Make a backup (just in case), then delete the Legacy file.
			if (FileUtil.saveFile(Preferences.prefsFile, "legacy", 0))
				Preferences.prefsFile.delete();
		}
	}

	/**
	 * Deduplicate list of Peers by combining values from matching UIDs. <br>
	 * For UI purposes, it is potentially important that existing IOPeers within the list exist for the current runtime.
	 * As such, this deduplication is used for saving, so duplicates are culled for future sessions.  <br>
	 *
	 * @return
	 */
	private ArrayList<IOPeer> deduplicate() {
		ArrayList<IOPeer> unique = new ArrayList<IOPeer>();
		peers.forEach(p -> {
			boolean add = true;
			for (IOPeer u : unique) {
				if (p.hasUID() && p.getUID().equals(u.getUID())) {
					// If this UID is already assigned to a Peer in the Unique List,
					// append this Peer's data to the existing Peer, and skip adding this Peer to the Unique List.
					add = false;
					p.copyTo(u);
					break;
				}
			}
			if (add)
				unique.add(p);
		});

		return unique;
	}

	/**
	 * Run through all log files, checking for new Peers.
	 */
	public void checkLogs() {
		for (File f : dbdLogDir.listFiles()) {
			if (f != null) {
				if (f.isDirectory())
					continue;
				if (!f.getName().endsWith(".log"))
					continue;
				System.out.println(f.getName());
				processLog(f, false);
			}
		}
		System.out.println("Identified " + peers.size() + " unique user/ip combos!");
		active = false;
	}

	/**
	 * Attempts to save the list of IOPeers.  <br>
	 * Should be called whenever a Peer's information is updated.  <br><br>
	 * Since the Peer List is static between all instances of PeerTracker, this method may be called by anything.
	 *
	 * @return True if this save works. May not work if a save is already underway.
	 */
	private boolean savePeers() {
		if (saving) {
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
			ps.save(deduplicate());
			saving = false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		saving = false;
		return false;
	}

	/**
	 * The main method of interfacing with the Peer List,
	 * this method either retrieves an existing IOPeer object which "owns" the given IP,
	 * or it returns the new IOPeer object generated - and containing - the new IP.
	 *
	 * @param ip
	 */
	public IOPeer getPeer(Inet4Address ip) {
		IOPeer ret = peers.stream().filter(p -> p.hasIP(ip)).findFirst().orElse(null);

		if (ret == null) {
			ret = new IOPeer();
			ret.addIP(ip);
			peers.add(ret);
		}
		if (!ret.hasUID())
			kindred.updatePeer(ret);

		return ret;
	}

	/**
	 * Iterates through the Log file, pairing UIDs and IPs that it can find,
	 * and adding them to the IOPeer list or updating existing IOPeers where missing info is found.
	 *
	 * @param f       The file to process.
	 * @param newFile If this file is new data, in a recently-created file.
	 */
	private void processLog(File f, final boolean newFile) {
		String lastID = "";
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String l;
			while ((l = br.readLine()) != null) {
				l = l.trim().toLowerCase();

				if (l.contains("connectionactive: 1"))
					active = true;
				else if (l.contains("connectionactive: 0"))
					active = false;

				if (!active) {
					uid = null;
					continue;
				}
				if (l.contains("steam: - id:")) {
					try {
						uid = l.substring(l.lastIndexOf('[') + 1);
						uid = uid.substring(0, uid.indexOf(']'));
						if (uid.length() < 17) {
							throw new IndexOutOfBoundsException();
						}
					} catch (IndexOutOfBoundsException e) {
						uid = null;
						System.err.println("Error parsing line: " + l);
					}
				}
				if (l.contains("-- ipaddress:")) {
					String ip = "";
					if (uid != null && active) {
						String[] addrSplit = l.split("address:");
						if (addrSplit.length < 2) { //This is caused by log files that end abruptly, such as a crash
							uid = null;
							active = false;
							continue;
						}
						ip = addrSplit[1].trim();
						if (ip.contains(":"))
							ip = ip.substring(0, ip.indexOf(":"));
						Inet4Address ina = null;
						try {
							ina = (Inet4Address) Inet4Address.getByName(ip);
							if (ina == null || (ina != null && (ina.isAnyLocalAddress() || ina.isSiteLocalAddress()))) {
								uid = null;
								active = false;
								continue;
							}

							boolean matched = false;
							for (IOPeer iop : peers) {
								if (uid.equals(iop.getUID()) || iop.hasIP(ina)) {
									if (!iop.hasUID()) {
										iop.setUID(uid);
									}
									if (!iop.hasIP(ina)) {
										iop.addIP(ina);
									}
									matched = true;
									if (newFile && !lastID.equals(iop.getUID().trim())) {
										lastID = iop.getUID().trim();
										kindred.addPeer(iop);
									}
									break;
								}
							}
							if (!matched) {
								IOPeer p = new IOPeer();
								p.setUID(uid);
								p.addIP(ina);
								peers.add(p);
								kindred.addPeer(p);
								lastID = p.getUID();
							}
						} catch (UnknownHostException e) {
							e.printStackTrace();
						} finally {
							active = false;
							uid = null;
						}
					}

				}
			}
			kindred.submit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
