package mlga.io.peer;

import java.io.Serializable;
import java.net.Inet4Address;
import java.util.concurrent.CopyOnWriteArrayList;

/** Wrapper used to save/load Peer settings. 
 * Must be serializable, so GSON can stream-encode them as objects.
 */
public class IOPeer implements Serializable{
	private transient static final long serialVersionUID = 5828536744242544871L;
	
	/** 
	 * Status is stored locally (and saved to file) using this Enum's values. <br>
	 * This allows for future changes without having to hunt down all calls to getStatus()
	 */
	public static enum Status implements Serializable{
		UNRATED, BLOCKED, LOVED;
	}

	/** When this peer was first discovered. */
	private long firstSeen;
	/** A version flag for each Peer object, for if something needs changing later. */
	public final int version = 1;
	/** List of all IPs this peer has been known under. */
	private CopyOnWriteArrayList<Integer> ips = new CopyOnWriteArrayList<Integer>();
	/** The UID of this peer. May not actually be set (other than null), if we haven't found them in a log file yet. */
	private String uid = "";
	/** This peer's status value, as set by the user. <br> Defaults to {@link Status.UNRATED}  */
	private Status status = Status.UNRATED;
	
	/** Flag toggled when this is created/modified. Toggles back to false by the Saver class once this Peer has been saved to file. */
	public transient boolean saved = false;
	
	public IOPeer(){
		this.firstSeen = System.currentTimeMillis();
	}
	
	
	/** Sets the UID of this peer, once we find it in the logs. */
	public void setUID(String uid){
		this.uid = uid.trim();
		this.saved = false;
	}
	
	/** Check if this IOPeer has a UID set for it or not. <br>
	 * Due to the nature of saving/loading, the UID will <b> not likely be null. </b> Use this check instead.
	 */
	public boolean hasUID(){
		return (this.uid!=null && !this.uid.equals(""));
	}
	
	/** Adds the given IP to this list. */
	public void addIP(Inet4Address ip){
		this.ips.addIfAbsent(formatIP(ip));
		this.saved = false;
	}
	
	/**
	 * Special method, used for building a peer from a Legacy list of IP Hashes.  <br>
	 * <b>Do not use this method outside of the Legacy conversion! </b><br>
	 * If the way that IOPeer handles IP hashing ever changes, this method will become obsolete and be removed.
	 * @param hash The pre-hashed IP.
	 */
	public void addLegacyIPHash(int hash){
		this.ips.add(hash);
	}
	
	/** Checks if this IOPeer contains the given IP address. */
	public boolean hasIP(Inet4Address ip){
		return ips.contains(formatIP(ip));
	}
	
	/**
	 * Wrapper function for converting INetAddrs into hashed IPs.  <br>
	 * Formatting is handled in this method to simplify keeping IP data the same everywhere.  <br>
	 * To modify the way IPs are saved, simply modify this method.
	 * @param ip The IP to convert.
	 * @return
	 */
	private int formatIP(Inet4Address ip){
		return ip.hashCode();
	}
	
	/** Sets this Peer's status to the int supplied. Check {@link IOPeer.Status} for values. */
	public void setStatus(Status stat){
		this.status = stat;
		this.saved = false;
	}
	
	/**
	 * Copies this IOPeer's data (IP List & Status) to the target IOPeer.  <br>
	 * This IOPeer's {@link #status} will overwrite p's.
	 * @param p The IOPeer object to copy this Peer's data to.
	 */
	public void copyTo(IOPeer p){
		p.ips.addAllAbsent(this.ips);
		p.setStatus(this.getStatus());
	}
	
	/** Get this peer's UID. <br>
	    See {@link #hasUID()} for possible values.
	 */
	public String getUID(){
		return this.uid;
	}
	
	/** Get the {@link Status} of this Peer, as set. */
	public Status getStatus(){
		return status;
	}
	
	/** {@link #firstSeen} is automatically set at creation. <br>
	 * It tracks when this Peer was first discovered, for posterity. */
	public long getFirstSeen(){
		return this.firstSeen;
	}

	public boolean equals(Object o){
		if(!(o instanceof IOPeer) )
			return false;
		IOPeer p2 = (IOPeer)o;
		if((getUID()== null && p2.getUID()!=null) || getUID()!=null && !getUID().equals(p2.getUID()) ){
			return false;
		}
		if(ips.size() != p2.ips.size())
			return false;
		for(int i = 0; i < ips.size(); i++){
			int v = ips.get(i).compareTo(p2.ips.get(i));
			if(v != 0)
				return false;
		}
		return true;
	}
	
}
