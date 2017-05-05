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
	
	public void save(){
		this.saved = Preferences.prefs.containsKey(this.id);
		if(saved){
			this.blocked = Preferences.prefs.get(this.id);
		}
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
