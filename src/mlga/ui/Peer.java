package mlga.ui;

import mlga.io.peer.IOPeer;

/** Simple wrapper to visually represent a Peer that is connected. */
public class Peer {
	private int id;
	private long ping = 0;
	private boolean blocked;
	private boolean hasStatus = false;
	private long last_seen;
	
	private IOPeer io;
	
	public Peer(int hash, long ttl, IOPeer io) {
		this.io = io;
		this.id = hash;
		this.hasStatus = io.getStatus()!=-1;
		if(this.hasStatus){
			this.blocked = io.getStatus()==0;
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
		return this.hasStatus && this.blocked;
	}
	
	/** Save our opinion of this Peer. */
	public void rate(boolean block){
		this.hasStatus = true;
		this.blocked = block;
		this.io.setStatus(block?0:1);
	}
	
	/** Remove this peer from the Preferences. */
	public void unsave(){
		this.hasStatus = false;
		this.io.setStatus(-1);
	}
	
	/** If we've saved this Peer before. */
	public boolean saved(){
		return this.hasStatus;
	}
	
	/** Returns the time (in milliseconds) since this Peer was last pinged. */
	public long age(){
		return System.currentTimeMillis() - this.last_seen;
	}
}
