package mlga.io.peer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Wrapper used to save/load Peer settings. 
 * Must be serializable, so GSON can stream-encode them as objects.*/
public class IOPeer implements Serializable{
	private static final long serialVersionUID = 5828536744242544871L;
	
	//TODO: https://sites.google.com/site/gson/streaming
	/** When this peer was first discovered. */
	private long firstSeen;
	
	/** List of all IPs this peer has been known under. <br>
	 * Initially will only contain one while still anon, but once this peer's name is matched we can detect more IPs they use in the future.
	 */
	private List<String> ips = new ArrayList<String>();
	/** The UID of this peer. May not actually be set (other than null), if we haven't found them in a log file yet. */
	private String uid = null;
	/** This peer's status; <br> -1 if this peer is unrated, 0 if blocked, 1 if loved. <br> Default is -1.*/
	private int status = -1;
	
	public IOPeer(){
		this.firstSeen = System.currentTimeMillis();
	}
	
	/** Sets the UID of this peer, once we find it in the logs. */
	public void setUID(String uid){
		this.uid = uid;
	}
	
	/** Adds the given IP to this list. */
	public void addIP(String ip){
		this.ips.add(ip);
	}
	
	/** Check to see if this Peer Object has been known under this UID or IP before. */
	public boolean matches(String ipOrUID){
		if(ipOrUID.equals(this.uid))
			return true;
		if(this.ips.contains(ipOrUID))
			return true;
		return false;
	}
	
	/** Sets this Peer's status to the int supplied. Check {@link #status} for values. */
	public void setStatus(int status){
		this.status = status;
	}
	
	/** Get this peer's UID. */
	public String getUID(){
		return this.uid;
	}
	
	/** Get the {@link #status} of this Peer, as set. */
	public int getStatus(){
		return this.status;
	}
	
	/** {@link #firstSeen} is automatically set at creation. <br>
	 * It tracks when this Peer was first discovered, for posterity. */
	public long getFirstSeen(){
		return this.firstSeen;
	}
	
}
