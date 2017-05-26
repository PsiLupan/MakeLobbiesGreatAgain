package mlga.io.peer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class PeerList {

	//method for opening stream (encryption later)
	//method for finding peer with Peer.matches function
	//method for saving peer list.
	//Method for adding new peer
	
	
	public static void savePeers(OutputStream out, List<IOPeer> peers) throws IOException {
		Gson gson = new Gson();
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.setIndent("    ");
		writer.beginArray();
		for (IOPeer message : peers) {
			gson.toJson(message, IOPeer.class, writer);
		}
		writer.endArray();
		writer.close();
	}


}
