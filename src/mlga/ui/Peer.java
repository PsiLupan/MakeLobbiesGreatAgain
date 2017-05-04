package mlga.ui;

import mlga.io.Preferences;

/** Simple wrapper to represent a Peer that is connected. */
public class Peer {
	private int id;
	private long ping = 0;
	private boolean loved;
	private boolean saved = false;
	private long last_seen;
	
	public Peer(int srcAddrHash, long rtt) {
		// TODO Auto-generated constructor stub
		this.id = srcAddrHash;
		this.saved = Preferences.prefs.containsKey(this.id);
		this.loved = this.saved && Preferences.prefs.get(this.id);
		this.last_seen = System.currentTimeMillis();
		System.err.println("Added new Peer: "+this.id);
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
	public boolean loved(){
		return this.saved && this.loved;
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
