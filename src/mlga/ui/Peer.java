package mlga.ui;

import mlga.io.Preferences;

/** Simple wrapper to represent a Peer that is connected. */
public class Peer {
	private int id;
	private long ping = 0;
	private boolean blocked;
	private boolean saved = false;
	private long last_seen;
	
	public Peer(int srcAddrHash, long rtt) {
		this.id = srcAddrHash;
		this.saved = Preferences.prefs.containsKey(this.id);
		if(this.saved){
			this.blocked = Preferences.prefs.get(this.id);
		}
		this.last_seen = System.currentTimeMillis();
	}
	
	public void setPing(long ping){
		this.ping = ping;
		this.last_seen = System.currentTimeMillis();
	}
	
	public int getID(){
		return this.id;
	}
	
	/** Returns this Peer's ping. */
	public long getPing(){
		return this.ping;
	}
	
	/** If we have saved, and also loved, this Peer. */
	public boolean blocked(){
		return this.saved && this.blocked;
	}
	
	/** Save our opinion of this Peer. */
	public void save(boolean block){
		this.saved = true;
		this.blocked = block;
		Preferences.set(this.id, false);
	}
	
	/** Remove this peer from the Preferences. */
	public void unsave(){
		this.saved = false;
		Preferences.remove(this.id);
	}
	
	/** If we've saved this Peer before. */
	public boolean saved(){
		return this.saved;
	}
	
	/** Returns the time (in milliseconds) since this Peer was last pinged. */
	public long age(){
		return System.currentTimeMillis() - this.last_seen;
	}
}
