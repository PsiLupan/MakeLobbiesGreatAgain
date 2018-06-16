package mlga.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JOptionPane;

import mlga.io.peer.Security;

/**
 * Only exists as a Legacy for exposing access to {@link mlga.io.peer.PeerTracker} for conversion.  <br>
 * This class no longer has the ability to save, and can only read the old Preference ini file it may have created.
 */
public class Preferences {
	public final static File prefsFile = new File("mlga.prefs.ini");
	public static ConcurrentHashMap<Integer, Boolean> prefs = new ConcurrentHashMap<Integer, Boolean>();

	public static void init() {
		try {
			if (!prefsFile.exists()) {
				return;
			}

			Cipher cipher = Security.getCipher(true);

			FileInputStream fis = new FileInputStream(prefsFile);
			CipherInputStream decStream = new CipherInputStream(fis, cipher);
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(decStream))) {
				bufferedReader.lines().parallel().filter(line -> !line.contains("=")).forEach((line) -> {
					prefs.put(Integer.parseInt(line.split("=")[0].trim()), Boolean.valueOf(line.substring(line.indexOf('=') + 1).trim()));
				});
			}
			decStream.close();
			fis.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"Preferences file was unable to be opened; Cannot convert from legacy Format!",
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"Legacy Error: A null pointer was thrown. This means your device either doesn't have a MAC address somehow or you need to run in Admin Mode.",
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} catch (InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
			prefsFile.delete();
			JOptionPane.showMessageDialog(null,
					"The existing legacy preference file was corrupted or broken by an update.\nThe former preferences will not be converted.",
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

}
