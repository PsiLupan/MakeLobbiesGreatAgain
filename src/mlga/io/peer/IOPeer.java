package mlga.io.peer;

import java.io.Serializable;
import java.net.Inet4Address;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wrapper used to save/load Peer settings.
 * Must be serializable, so GSON can stream-encode them as objects.
 */
public class IOPeer implements Serializable {

	/**
	 * Status is stored locally (and saved to file) as an int, for potential future-proofing. <br>
	 * However, all external accessors for Status use these values. <br>
	 * This allows for future changes without having to hunt down all calls to getStatus()
	 */
	public enum Status {
		UNRATED(-1), BLOCKED(0), LOVED(1);

		public final int val;

		Status(int value) {
			this.val = value;
		}
	}

	/** When this peer was first discovered. */
	private long firstSeen;
	/** A version flag for each Peer object, for if something needs changing later. */
	public final int version = 1;
	/** List of all IPs this peer has been known under. */
	private CopyOnWriteArrayList<Integer> ips = new CopyOnWriteArrayList<Integer>();
	/** The UID of this peer. May not actually be set (default "" to avoid NPEs), if we haven't found them in a log file yet. */
	private String uid = "";
	/** This peer's status value, stored as an integer for any future updates/refactoring. */
	private int status = Status.UNRATED.val;

	/** Flag automatically set when this IOPeer is created/modified and requires saving. <br> Toggled back to true by the Saver class once this Peer has been saved to file. */
	public transient boolean saved = false;

	public IOPeer() {
		this.firstSeen = System.currentTimeMillis();
	}


	/** Sets the UID of this peer, once we find it in the logs. */
	public void setUID(String uid) {
		this.uid = uid.trim();
		this.saved = false;
	}

	/**
	 * Check if this IOPeer has a UID set for it or not. <br>
	 * Due to the nature of saving/loading, the UID will <b> not likely be null. </b> Use this check instead.
	 */
	public boolean hasUID() {
		return (this.uid != null && !this.uid.equals("") && this.uid.length() > 16);
	}

	/**
	 * Adds the given IP to this list.  <br>
	 * Will not add duplicates.
	 */
	public void addIP(Inet4Address ip) {
		this.ips.addIfAbsent(formatIP(ip));
		this.saved = false;
	}

	/**
	 * Special method, used for building a Peer from server responses or Legacy IPs, which are already hashed.  <br>
	 * Will not add duplicates.  <br>
	 * If the way that IOPeer handles IP hashing ever changes, Legacy IPs must be discontinued.
	 *
	 * @param hash The pre-hashed IP.
	 */
	public void addPrehashedIP(int hash) {
		this.ips.addIfAbsent(hash);
		this.saved = false;
	}

	/** Checks if this IOPeer contains the given IP address. */
	public boolean hasIP(Inet4Address ip) {
		return ips.contains(formatIP(ip));
	}

	/**
	 * Wrapper function for converting INetAddrs into hashed IPs.  <br>
	 * Formatting is handled in this method to simplify keeping IP data the same everywhere.  <br>
	 * To modify the way IPs are saved, simply modify this method.
	 *
	 * @param ip The IP to convert.
	 *
	 * @return
	 */
	private int formatIP(Inet4Address ip) {
		int hash = 0;
		int len = ip.getAddress().length;
		int i = len > 4 ? len - 4 : 0;
		for (; i < len; i++)
			hash = (hash << 8) | (ip.getAddress()[i] & 0xff);
		return hash;
	}

	/** Sets this Peer's status to the int supplied. Check {@link IOPeer.Status} for values. */
	public void setStatus(Status stat) {
		this.status = stat.val;
		this.saved = false;
	}

	/**
	 * Copies this IOPeer's data (IP List & Status) to the target IOPeer.  <br>
	 * This IOPeer's {@link #status} will overwrite p's if it has been set.
	 *
	 * @param p The IOPeer object to copy this Peer's data to.
	 */
	public void copyTo(IOPeer p) {
		p.ips.addAllAbsent(this.ips);
		if (this.getStatus() != Status.UNRATED) {
			p.setStatus(this.getStatus());
		}
		p.saved = false;
	}

	/**
	 * Build an array if the IPs currently stored by this IOPeer.  <br>
	 * This is *only* to be used in places where the IP List must be submitted raw.
	 *
	 * @return An immutable array of Integer IPs.
	 */
	public int[] getIPs() {
		int[] ret = this.ips.stream().mapToInt(i -> i).toArray();
		return ret;
	}

	/**
	 * Get this peer's UID. <br>
	 * See {@link #hasUID()} for possible values.
	 */
	public String getUID() {
		return this.uid;
	}

	/** Get the {@link IOPeer.Status} of this Peer, as set by the user. */
	public Status getStatus() {
		for (Status s : Status.values()) {
			if (s.val == this.status) {
				return s;
			}
		}
		System.err.println("Unknown status value: " + this.status);
		return Status.UNRATED;
	}

	/**
	 * {@link #firstSeen} is automatically set at creation. <br>
	 * It tracks when this Peer was first discovered, for posterity.
	 */
	public long getFirstSeen() {
		return this.firstSeen;
	}

	public boolean equals(Object o) {
		if (!(o instanceof IOPeer)) {
			return false;
		}
		IOPeer p2 = (IOPeer) o;
		if ((getUID() == null && p2.getUID() != null) || getUID() != null && !getUID().equals(p2.getUID())) {
			return false;
		}
		if (ips.size() != p2.ips.size()) {
			return false;
		}
		for (int i = 0; i < ips.size(); i++) {
			int v = ips.get(i).compareTo(p2.ips.get(i));
			if (v != 0) {
				return false;
			}
		}
		return true;
	}

}
