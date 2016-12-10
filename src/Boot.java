import java.awt.AWTException;
import java.awt.GridLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UnsupportedLookAndFeelException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;

public class Boot {
	private static InetAddress addr = null;

	public static void main(String[] args){
		try {
			Sanity.check();
			setupTray();

			getLocalAddr();
			PcapNetworkInterface nif = Pcaps.getDevByAddress(addr);

			int snapLen = 65536;
			PromiscuousMode mode = PromiscuousMode.PROMISCUOUS;
			int timeout = 0;
			PcapHandle handle = nif.openLive(snapLen, mode, timeout);

			short pckCount = 0;
			long lastPacketTime = 0;
			Timestamp requestTime = null;
			boolean expectPong = false;
			String currSrv = null;
			Overlay ui = new Overlay();

			while(true){
				Packet packet = handle.getNextPacket();

				if(packet != null){
					IpV4Packet ippacket = packet.get(IpV4Packet.class);

					if(ippacket != null){
						if(ippacket.getHeader().getProtocol() == IpNumber.UDP){
							UdpPacket udppack = ippacket.get(UdpPacket.class);
							String srcAddrStr = ippacket.getHeader().getSrcAddr().toString(); // Shows as '/0.0.0.0'
							
							if(!srcAddrStr.equals(currSrv)){
								if(udppack.getPayload().getRawData().length == 56 && ippacket.getHeader().getSrcAddr().isSiteLocalAddress()){
									requestTime = handle.getTimestamp();
									expectPong = true;
								}
								else if(udppack.getPayload().getRawData().length == 68 && !ippacket.getHeader().getSrcAddr().isSiteLocalAddress()){
									if(lastPacketTime == 0 || System.currentTimeMillis() - lastPacketTime < 10000){
										pckCount++;

										if(pckCount == 4){ //The new packet is sent multiple times, we only really need 4 to confirm
											ui.setKillerLocale("...");
											geolocate(srcAddrStr, ui);
											currSrv = srcAddrStr; //This serves to prevent seeing the message upon joining then leaving
											pckCount = 0;
										}
									}else{ //If the packets take more than 10 secs, probably wrong packet
										lastPacketTime = 0;
										pckCount = 0;
									}	
								}
							}else{
								if(expectPong && udppack.getPayload().getRawData().length == 68 && ippacket.getHeader().getDstAddr().isSiteLocalAddress()){
									ui.setPing(handle.getTimestamp().getTime() - requestTime.getTime());
									expectPong = false;
								}
							}
						}
					}
				}
			}
		} catch (PcapNativeException | NotOpenException 
				| ClassNotFoundException | InstantiationException 
				| IllegalAccessException | UnsupportedLookAndFeelException 
				| IOException | ParseException 
				| AWTException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static void setupTray() throws AWTException{
		final SystemTray tray = SystemTray.getSystemTray();

		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
		PopupMenu popup = new PopupMenu();
		MenuItem exit = new MenuItem();
		exit.addActionListener(listener);
		exit.setLabel("Exit");
		popup.add(exit);
		tray.add(new TrayIcon(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("resources/icon.png")), "MLGA", popup));
	}

	public static void geolocate(String ip, Overlay ui) throws IOException, ParseException{
		String code = null;
		Long proxy = 0L;

		try (InputStream is = new URL("http://legacy.iphub.info/api.php?showtype=4&ip=" + ip.replace("/", "")).openStream();
				BufferedReader buf = new BufferedReader(new InputStreamReader(is))) {
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(buf);
			if(ui.useCountryName()){
				code = (String)obj.get("countryName");
			}else{
				code = (String)obj.get("countryCode");
			}
			proxy = (Long)obj.get("proxy");
		}
		ui.setKillerLocale(code);
		if(proxy != 0L){ //Could abuse anything non-zero being true, but probably shouldn't.
			ui.setProxy(true);
		}else{
			ui.setProxy(false);
		}

	}

	public static void getLocalAddr() throws InterruptedException, PcapNativeException{
		final JFrame frame = new JFrame("MLGA Network Device Locate");
		frame.setFocusableWindowState(true);

		JLabel ipLab = new JLabel("Enter LAN IP:", JLabel.LEFT);
		JLabel exLab = new JLabel("(Ex. 192.168.0.2 or 10.0.0.2, obtained from Network Settings)", JLabel.LEFT);
		final JComboBox<String> lanIP = new JComboBox<String>();

		for(PcapNetworkInterface i : Pcaps.findAllDevs()){
			for(PcapAddress x : i.getAddresses()){
				if(x.getAddress() != null && x.getNetmask() != null){
					lanIP.addItem(x.getAddress().getHostAddress());
				}
			}
		}

		if(lanIP.getItemCount() == 0){
			lanIP.addItem("No devices found. Try running in Admin mode.");
		}

		JButton start = new JButton("Start");
		start.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					addr = InetAddress.getByName((String)lanIP.getSelectedItem());
					frame.setVisible(false);
					frame.dispose();
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
			}
		});

		frame.setLayout(new GridLayout(4,1));
		frame.add(ipLab);
		frame.add(exLab);
		frame.add(lanIP);
		frame.add(start);
		frame.setAlwaysOnTop(true);
		frame.pack();
		frame.setLocation(5, 420);
		frame.setSize(400, 150);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		while(frame.isVisible()){
			Thread.sleep(10);
		}
	}
}
