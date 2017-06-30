package mlga.ui;

import java.net.Inet4Address;

/** Simple wrapper to visually represent a Peer that is connected. */
public class Peer {
	private Inet4Address id;
	private long ping = 0;
	private long last_seen;

	public Peer(Inet4Address hash, long ttl) {
		this.id = hash;
		this.last_seen = System.currentTimeMillis();
	}
	
	public void setPing(long ping){
		this.ping = ping;
		this.last_seen = System.currentTimeMillis();
	}
	
	public Inet4Address getID(){
		return this.id;
	}
	
	/** Returns this Peer's ping. */
	public long getPing(){
		return this.ping;
	}
	
	/** Returns the time (in milliseconds) since this Peer was last pinged. */
	public long age(){
		return System.currentTimeMillis() - this.last_seen;
	}
}
