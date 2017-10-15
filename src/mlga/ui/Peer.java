package mlga.ui;

import java.net.Inet4Address;

import mlga.io.peer.IOPeer;
import mlga.io.peer.IOPeer.Status;

/** Simple wrapper to visually represent a Peer that is connected. */
public class Peer {
	private Inet4Address id;
	private long ping = 0;
	private boolean blocked;
	private boolean hasStatus = false;
	private long last_seen;

	private IOPeer io;

	public Peer(Inet4Address hash, long ttl, IOPeer io) {
		this.io = io;
		this.id = hash;
		this.hasStatus = io.getStatus() != Status.UNRATED;
		if (this.hasStatus) {
			this.blocked = io.getStatus() == Status.BLOCKED;
		}
		this.last_seen = System.currentTimeMillis();
	}

	public void setPing(long ping) {
		this.ping = ping;
		this.last_seen = System.currentTimeMillis();
	}

	public Inet4Address getID() {
		return this.id;
	}

	/** Returns this Peer's ping. */
	public long getPing() {
		return this.ping;
	}

	/** If we have saved, and also blocked, this Peer. */
	public boolean blocked() {
		return this.hasStatus && this.blocked;
	}

	/** Save our opinion of this Peer. */
	public void rate(boolean block) {
		this.hasStatus = true;
		this.blocked = block;
		this.io.setStatus(block ? Status.BLOCKED : Status.LOVED);
	}

	/** Remove this peer from the Preferences. */
	public void unsave() {
		this.hasStatus = false;
		this.io.setStatus(Status.UNRATED);
	}

	/** If we've saved this Peer before. */
	public boolean saved() {
		return this.hasStatus;
	}

	/** Returns the time (in milliseconds) since this Peer was last pinged. */
	public long age() {
		return System.currentTimeMillis() - this.last_seen;
	}
}
