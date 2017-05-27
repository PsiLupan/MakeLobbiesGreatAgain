package mlga.io.peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import mlga.io.FileUtil;

public class PeerSaver {
	private final File saveFile;
	
	/**
	 * Builds a new {@link #PeerSaver(File)} Object, which handles encryption and saving
	 * IOPeer lists to the given file.
	 * @param save The file to use for saving.
	 */
	public PeerSaver(File save){
		this.saveFile = save;
	}
	
	/**
	 * Saves the given list of Peers to this Saver's file.
	 * @param peers The list to save.
	 * @return True if saving works properly.
	 */
	public boolean save(List<IOPeer> peers){
		try{
			// Keep a rolling backup of the Peers file, for safety.
			if(this.saveFile.exists()){
				FileUtil.saveFile(this.saveFile, "", 1);
			}
			this.savePeers(openStream(this.saveFile), peers);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	/** This method handles opening an OutputStream to the given file.
	 * @param f The file to open.
	 * @return The stream opened to the desired file.
	 * @throws FileNotFoundException 
	 */
	private OutputStream openStream(File f) throws FileNotFoundException{
		return new FileOutputStream(saveFile);
		//TODO: Encryption instead of raw stream after debugging's finished.
	}
	
	private void savePeers(OutputStream out, List<IOPeer> peers) throws IOException {
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
