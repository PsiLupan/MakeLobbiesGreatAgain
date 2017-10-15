package mlga.io.peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import javax.crypto.CipherInputStream;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/**
 * Simple class to handle reading IOPeer objects from the given file.  <br>
 * Handles decryption as needed on the file.
 *
 * @author ShadowMoose
 */
public class PeerReader {
	private JsonReader reader;
	private Gson gson;

	/** Builds a Peer Reader for the given File. */
	public PeerReader(File f) throws IOException {
		if (!f.createNewFile()) {
			this.gson = new Gson();
			reader = new JsonReader(open(f));
			reader.beginArray();
		}
	}

	/** Gets the next unread IOPeer in the list, or null if none remain. */
	public IOPeer next() throws IOException {
		if (reader.hasNext()) {
			IOPeer peer = gson.fromJson(reader, IOPeer.class);
			peer.saved = true;
			return peer;
		}
		return null;
	}

	/** If this PeerStream has more IOPeers left to read. */
	public boolean hasNext() {
		try {
			if (reader == null) {
				return false;
			} else if (!reader.hasNext()) {
				this.close();
				return false;
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void close() throws IOException {
		reader.endArray();
		reader.close();
	}

	private InputStreamReader open(File f) throws IOException {
		CipherInputStream decStream = null;
		FileInputStream fis = new FileInputStream(f);
		try {
			decStream = new CipherInputStream(fis, Security.getCipher(true));
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException();
		}
		return new InputStreamReader(new GZIPInputStream(decStream), "UTF-8");
	}
}
