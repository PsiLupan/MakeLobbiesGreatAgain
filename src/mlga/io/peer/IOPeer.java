package mlga.io.peer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Wrapper used to save/load Peer settings. 
 * Must be serializable, so GSON can stream-encode them as objects.
 */
public class IOPeer implements Serializable{
	private transient static final long serialVersionUID = 5828536744242544871L;
	
	/** When this peer was first discovered. */
	private long firstSeen;
	/** A version flag for each Peer object, for if something needs changing later. */
	public final int version = 1;
	/** List of all IPs this peer has been known under. */
	private List<String> ips = new ArrayList<String>();
	/** The UID of this peer. May not actually be set (other than null), if we haven't found them in a log file yet. */
	private String uid = null;
	/** This peer's status; <br> -1 if this peer is unrated, 0 if blocked, 1 if loved. <br> Default is -1.*/
	private int status = -1;
	
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
	public void addIP(String ip){
		this.ips.add(ip.trim());
		this.saved = false;
	}
	
	/** Checks if this IOPeer contains the given IP address. */
	public boolean hasIP(String ip){
		return ips.contains(ip.trim());
	}
	
	/** Check to see if this Peer Object has been known under this UID or IP before. */
	public boolean matches(String ipOrUID){
		if(ipOrUID.trim().equals(this.uid))
			return true;
		if(hasIP(ipOrUID))
			return true;
		return false;
	}
	
	/** Sets this Peer's status to the int supplied. Check {@link #status} for values. */
	public void setStatus(int status){
		this.status = status;
		this.saved = false;
	}
	
	/** Get this peer's UID. <br>
	    See {@link #hasUID()} for possible values.
	 */
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

	public boolean equals(Object o){
		if(!(o instanceof IOPeer) )
			return false;
		IOPeer p2 = (IOPeer)o;
		if(!getUID().equals(p2.getUID()) ){
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
