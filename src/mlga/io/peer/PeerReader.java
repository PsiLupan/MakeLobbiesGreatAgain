package mlga.io.peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/**
 * Simple class to handle reading IOPeer objects from the given file.  <br>
 * Handles decryption as needed on the file.
 * @author ShadowMoose
 *
 */
public class PeerReader {
	private JsonReader reader;
	private Gson gson;
	
	/** Builds a Peer Reader for the given File. */
	public PeerReader(File f) throws IOException{
		this.gson = new Gson();
		reader = new JsonReader(open(f));
		reader.beginArray();
	}
	
	/** Gets the next unread IOPeer in the list, or null if none remain. */
	public IOPeer next() throws IOException {
		if (reader.hasNext()) {
			IOPeer peer = gson.fromJson(reader, IOPeer.class);
			return peer;
		}
		reader.endArray();
		reader.close();
		return null;
	}
	
	/** If this PeerStream has more IOPeers left to read. */
	public boolean hasNext(){
		try {
			return reader.hasNext();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void close() throws IOException{
		reader.endArray();
		reader.close();
	}

	private InputStreamReader open(File f) throws FileNotFoundException, UnsupportedEncodingException{
		//TODO: Encryption.
		return new InputStreamReader(new FileInputStream(f), "UTF-8");
	}
}
