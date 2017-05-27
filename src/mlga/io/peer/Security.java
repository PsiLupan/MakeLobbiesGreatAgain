package mlga.io.peer;

import java.net.NetworkInterface;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import mlga.Boot;
/**
 * Class to simplify key generation/file access.
 * @author ShadowMoose
 */
public class Security {
	
	/**
	 * Builds the Cipher Object userd for encryption/decryption.
	 * @param readMode The Cipher will be initiated in either encrypt/decrypt mode.
	 * @return Cipher object, ready to go.
	 * @throws Exception Many possible issues can arise, so this is a catch-all.
	 */
	public static Cipher getCipher(boolean readMode) throws Exception{
		SecretKey desKey;
		Cipher cipher = null;
		byte[] mac = new byte[8];
		int i = 0;
		if(Boot.nif.getLinkLayerAddresses().get(0) != null){
			for(byte b : Boot.nif.getLinkLayerAddresses().get(0).getAddress()){
				mac[i] = b;
				i++;
			}
		}else{
			for(byte b : NetworkInterface.getNetworkInterfaces().nextElement().getHardwareAddress()){
				mac[i] = b;
				i++;
			}
		}
		mac[6] = 'W';
		mac[7] = 'C';

		DESKeySpec key = new DESKeySpec(mac);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		desKey = keyFactory.generateSecret(key);
		cipher = Cipher.getInstance("DES");

		if(readMode){
			cipher.init(Cipher.DECRYPT_MODE, desKey);
		}else{
			cipher.init(Cipher.ENCRYPT_MODE, desKey);
		}
		return cipher;
	}
}
