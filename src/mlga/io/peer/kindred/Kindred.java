package mlga.io.peer.kindred;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mlga.io.Settings;
import mlga.io.peer.IOPeer;
import mlga.ui.LoginPanel;

/**
 * The Kindred System enables mapping & tracking of PeerID->IP through the database,
 * which is curated online by all registered users.  <br>
 * All data is kept anonymous, and personal user ratings are not reported to the server.
 * @author ShadowMoose
 */
public class Kindred {
	private final String KINDRED_VERSION = "1";
	private final String submit_peer_url = "https://mlga.rofl.wtf/submit_report.php";
	private final String lookup_peer_url = "https://mlga.rofl.wtf/check_ip.php";
	
	private String token = null;
	private JsonArray queue = new JsonArray();
	private boolean saving = false;
	
	/**
	 * Initializes this Kindred System object.  <br>
	 * Will prompt the user for authentication if they haven't been previously prompted.
	 */
	public Kindred(){
		prompt();
	}
	
	/**
	 * Prompts the user for the Kindred token, if one isn't set already.
	 */
	private void prompt(){
		if(Settings.get("kindred_token", null) == null){
			LoginPanel lp = new LoginPanel();
			lp.prompt();
			if(lp.getCode() != null){
				Settings.set("kindred_token", lp.getCode());
			}else{
				Settings.set("kindred_token", "false");
			}
		}
		this.token = Settings.get("kindred_token", "false");
		if(this.token.equals("false"))
			this.token = null;
	}
	
	/**
	 * Look up the given IP hash, and adds the missing information to the given IOPeer.
	 * @param iop The IOPeer to be mutated.
	 */
	public void updatePeer(IOPeer iop){
		if(iop.hasUID())
			return;
		System.out.println("KINDRED: Attempting to ID Peer through KINDRED...");
		
		JsonArray ar = new JsonArray();
		for(int i : iop.getIPs()){
			ar.add(i);
		}
		JsonObject re = this.postToUrl(lookup_peer_url, ar, "peer_ips");
		if(re == null){
			//System.err.println("KINDRED: Invalid server response!");
			return;
		}
		if(re.get("success") == null){
			System.err.println("KINDRED: Could not identify Peer.");
			return;
		}
		iop.setUID(re.get("uid").getAsString());
		re.getAsJsonArray("known_ips").forEach((r) ->{
			iop.addPrehashedIP(r.getAsInt());;
		});
	}
	
	/**
	 * Adds the IOPeer's details to this Kindred object, and prepares to submit when called.
	 * @param iop The IOPeer to submit to KINDRED.
	 */
	public void addPeer(IOPeer iop){
		if(this.token == null)
			return;
		if(!iop.hasUID())
			return;
		
		for (int ip : iop.getIPs()){
			JsonObject ob = new JsonObject();
			ob.addProperty("uid", iop.getUID());
			ob.addProperty("ip", ip);
			this.queue.add(ob);
		}
	}
	
	/**
	 * Indicates to Kindred that the list should attempt to be submitted to the DB in the near future.  <br>
	 * Kindred will delay for up to several seconds to allow the list to batch.  <br>
	 * 
	 * Calling this while Kindred is already saving will do nothing.
	 */
	public void submit(){
		if(this.token == null || this.saving || this.queue.size() == 0)
			return;
		System.out.println("KINDRED: Upload queued.");
		this.saving = true;
		Thread t = new Thread("Kindred"){
			public void run(){
				try{
					Thread.sleep(2000);
					System.out.println("KINDRED: Submitting data for ~["+Kindred.this.queue.size()+"] Peers...");
					if(!Kindred.this.post())
						System.err.println("KINDRED: Error submitting data.");
				}catch(Exception e){
					e.printStackTrace();
				}
				Kindred.this.saving = false;
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * Submit the given list of IOPeers we've identified to the Kindred database.
	 * @return
	 */
	private boolean post(){
		JsonObject resp = this.postToUrl(submit_peer_url, this.queue, "manifest");
		if(resp == null){
			System.err.println("KINDRED: Invalid server response.");
			return false;
		}
		System.out.println("KINDRED: "+resp.get("success").getAsString());
		return true;
	}
	
	/**
	 * Formats and POSTs the given JsonArray to the supplied URL.  <br>
	 * GZips and Base64_encodes the data to a string.  <br>
	 * Additionally handles error checking and token invalidation.
	 * @param target The URL to POST to.
	 * @param arr The Data Array to provide.
	 * @return Expects a JSON response, and returns a parsed JsonObject (or null on error/missing token).
	 */
	private JsonObject postToUrl(String target, JsonArray arr, String array_key){
		if(this.token == null)
			return null;
		String out = null;
		try{
			//It's JSON data, so it's likely worth compressing:
			ByteArrayOutputStream obj = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(obj);
			gzip.write(new Gson().toJson(arr).getBytes("UTF-8"));
			gzip.close();
			out = Base64.getEncoder().encodeToString(obj.toByteArray());
		}catch(IOException e){
			e.printStackTrace();
		}
		if(out == null){
			System.err.println("KINDRED: Error encoding!");
			return null;
		}
		Map<String,String> params = new HashMap<>();
		params.put("token", this.token);
		params.put("kindred_version", this.KINDRED_VERSION);
		params.put(array_key, out);
		this.queue = new JsonArray();

		try{
			StringBuilder postData = new StringBuilder();
			for (Map.Entry<String,String> param : params.entrySet()) {
				if (postData.length() != 0) 
					postData.append('&');
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
			}
			//System.out.println(postData.toString());
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");
			URL url = new URL(target);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			conn.setDoOutput(true);
			conn.getOutputStream().write(postDataBytes);
			
			InputStream is = conn.getInputStream();
			JsonElement ele = new JsonParser().parse(new InputStreamReader(is) );
			is.close();
			JsonObject resp = ele.getAsJsonObject();
			if(resp.get("reset_token") != null && resp.get("reset_token").getAsBoolean()){
				System.err.println("KINDRED: Resetting token.");
				this.token = null;
				Settings.remove("kindred_token");
				this.prompt();
			}
			if(resp.get("error") != null){
				System.err.println("KINDRED: "+resp.get("error").getAsString());
				return null;
			}
			return resp;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
