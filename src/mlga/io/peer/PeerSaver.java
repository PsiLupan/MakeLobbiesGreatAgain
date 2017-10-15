package mlga.io.peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import mlga.io.FileUtil;

/**
 * Class for saving lists of IOPeers to JSON format.  <br>
 * Handles output File encryption natively within the class as needed.
 *
 * @author ShadowMoose
 */
public class PeerSaver {
	private final File saveFile;

	/**
	 * Builds a new {@link #PeerSaver(File)} Object, which handles encryption and saving
	 * IOPeer lists to the given file.
	 *
	 * @param save The file to use for saving.
	 */
	public PeerSaver(File save) {
		this.saveFile = save;
	}

	/**
	 * Saves the given list of Peers to this Saver's file.  <br>
	 * Automatically creates a backup file first if a save already exists.
	 *
	 * @param peers The list to save.
	 *
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void save(List<IOPeer> peers) throws FileNotFoundException, IOException {
		// Keep a rolling backup of the Peers file, for safety.
		if (this.saveFile.exists()) {
			FileUtil.saveFile(this.saveFile, "", 1);
		}
		this.savePeers(openStream(this.saveFile), peers);
	}


	/**
	 * This method handles opening an OutputStream to the given file.
	 *
	 * @param f The file to open.
	 *
	 * @return The stream opened to the desired file.
	 *
	 * @throws IOException
	 */
	private OutputStream openStream(File f) throws IOException {
		Cipher c;
		try {
			c = Security.getCipher(false);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException();
		}
		return new GZIPOutputStream(new CipherOutputStream(new FileOutputStream(saveFile), c));
	}

	private void savePeers(OutputStream out, List<IOPeer> peers) throws IOException {
		Gson gson = new Gson();
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.setIndent("");
		writer.beginArray();
		peers.forEach(p -> {
			gson.toJson(p, IOPeer.class, writer);
			p.saved = true;
		});
		writer.endArray();
		writer.close();
	}


}
