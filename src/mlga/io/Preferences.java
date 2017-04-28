package mlga.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.swing.JOptionPane;

import mlga.Boot;

public class Preferences {
	
	private static SecretKey desKey;
	private static Cipher cipher;
	private final static File prefsFile = new File("mlga.prefs.ini");
	public static ConcurrentHashMap<Integer, Boolean> prefs = new ConcurrentHashMap<Integer, Boolean>();
	
	public static void init(){
		try{
			if(!prefsFile.exists()){
				prefsFile.createNewFile();
				System.err.println("Blocks file does not exist. Creating file.");
			}
			
			
			byte[] mac = new byte[8];
			short i = 0;
			for(byte b : NetworkInterface.getByInetAddress(Boot.addr).getHardwareAddress()){
				mac[i] = b;
			}
			mac[6] = 'W';
			mac[7] = 'C';
			DESKeySpec key = new DESKeySpec(mac);
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
	        desKey = keyFactory.generateSecret(key);
	        cipher = Cipher.getInstance("DES");
	        
	        cipher.init(Cipher.DECRYPT_MODE, desKey);
			FileInputStream fis = new FileInputStream(prefsFile);
			CipherInputStream decStream = new CipherInputStream(fis, cipher);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(decStream));
			
			String line="";
			while ((line = bufferedReader.readLine()) != null){
				if(line.trim().startsWith(";") || !line.contains("=")){
					continue;//skip comments. It's INI, but we'll still pretend to follow conventions.
				}
				prefs.put(Integer.parseInt(line.split("=")[0].trim()), Boolean.valueOf(line.substring(line.indexOf('=')+1).trim()));
			}
			for(Integer k : prefs.keySet() ){
				System.out.println((k.toString().trim()+"="+prefs.get(k)));
			}
			bufferedReader.close();
		}catch(IOException | InvalidKeyException | InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException e){
			e.printStackTrace();
			prefsFile.delete();
			JOptionPane.showMessageDialog(null, 
					"No preferences were able to be loaded in or the existing preference file was corrupted.\nThe existing file has been deleted.\nPlease restart the application.", 
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}
	
	public static void remove(Integer ipHash){
		prefs.remove(ipHash);
		FileOutputStream o;
		try {
			o = new FileOutputStream(prefsFile);
			cipher.init(Cipher.ENCRYPT_MODE, desKey);
			for(Integer k : prefs.keySet() ){
				o.write(cipher.doFinal((k.toString().trim()+"="+Boolean.toString(prefs.get(k))+"\r\n").getBytes()));
			}
			o.close();
		} catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
	}
	
	public static void set(Integer key, Boolean state){
		prefs.remove(key);
		prefs.put(key, state);
		FileOutputStream o;
		try {
			o = new FileOutputStream(prefsFile);
			cipher.init(Cipher.ENCRYPT_MODE, desKey);
			for(Integer k : prefs.keySet() ){
				o.write(cipher.doFinal((k.toString().trim()+"="+Boolean.toString(prefs.get(k))+"\r\n").getBytes()));
			}
			o.close();
		} catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
	}
}
