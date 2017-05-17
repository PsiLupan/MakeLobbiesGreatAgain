package mlga;

import java.awt.AWTException;
import java.awt.FontFormatException;
import java.awt.GridLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UnsupportedLookAndFeelException;

import org.pcap4j.core.BpfProgram;
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

import mlga.io.Settings;
import mlga.ui.Overlay;

public class Boot {
	public static PcapNetworkInterface nif = null;

	private static InetAddress addr = null;
	private static PcapHandle handle = null;

	public static void main(String[] args) throws UnsupportedLookAndFeelException, AWTException, ClassNotFoundException, InterruptedException,
	FontFormatException, InstantiationException, IllegalAccessException, IOException, PcapNativeException, NotOpenException {
		if(!Sanity.check()){
			System.exit(1);
		}
		Settings.init();
		Settings.set("autoload", Settings.get("autoload", "0")); //"autoload" is an ini-only toggle for advanced users.
		setupTray();

		getLocalAddr();
		nif = Pcaps.getDevByAddress(addr);
		if(nif == null){
			JOptionPane.showMessageDialog(null, "The device you selected doesn't seem to exist. Double-check the IP you entered.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		final int addrHash = addr.hashCode();
		final int snapLen = 65536;
		final PromiscuousMode mode = PromiscuousMode.NONPROMISCUOUS;
		final int timeout = 0;
		handle = nif.openLive(snapLen, mode, timeout);
		handle.setFilter("udp", BpfProgram.BpfCompileMode.OPTIMIZE);

		HashMap<Integer, Timestamp> active = new HashMap<Integer, Timestamp>();
		final Overlay ui = new Overlay();

		while(true){				
			final Packet packet = handle.getNextPacket();

			if(active.size() > 5){ //Keep active from filling and keeps it sep. from the TimerTask for thread safety
				active.clear();
				ui.clearPeers();
			}

			if(packet != null){
				final IpV4Packet ippacket = packet.get(IpV4Packet.class);

				if(ippacket != null){
					final UdpPacket udppack = ippacket.get(UdpPacket.class);

					if(udppack != null && udppack.getPayload() != null){
						final int srcAddrHash = ippacket.getHeader().getSrcAddr().hashCode();
						final int dstAddrHash = ippacket.getHeader().getDstAddr().hashCode();
						final int payloadLen = udppack.getPayload().getRawData().length;

						if(active.containsKey(srcAddrHash) && srcAddrHash != addrHash){
							if(active.get(srcAddrHash) != null && payloadLen == 68  //Packets are STUN related: 56 is request, 68 is response
									&& dstAddrHash == addrHash){
								ui.setPing(srcAddrHash, handle.getTimestamp().getTime() - active.get(srcAddrHash).getTime());
								active.put(srcAddrHash, null); //No longer expect ping
							}
						}else{
							if(payloadLen == 56 && srcAddrHash == addrHash){
								active.put(ippacket.getHeader().getDstAddr().hashCode(), handle.getTimestamp());
							}
						}
					}
				}
			}
		}
	}

	public static void setupTray() throws AWTException{
		final SystemTray tray = SystemTray.getSystemTray();
		final PopupMenu popup = new PopupMenu();
		final MenuItem exit = new MenuItem();
		final TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("resources/icon.png")), "MLGA", popup);

		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handle.close();
				System.exit(0);
			}
		});
		exit.setLabel("Exit");
		popup.add(exit);
		tray.add(trayIcon);
	}

	public static void getLocalAddr() throws InterruptedException, PcapNativeException, UnknownHostException, SocketException{
		if(Settings.getDouble("autoload", 0) == 1){
			addr = InetAddress.getByName(Settings.get("addr", ""));
			return;
		}
		final JFrame frame = new JFrame("MLGA Network Device Locate");
		frame.setFocusableWindowState(true);

		final JLabel ipLab = new JLabel("Select LAN IP obtained from Network Settings:", JLabel.LEFT);
		final JComboBox<String> lanIP = new JComboBox<String>();
		final JLabel lanLabel = new JLabel("If your device IP isn't in the dropdown, provide it below.");
		final JTextField lanText = new JTextField(Settings.get("addr", ""));

		ArrayList<InetAddress> inets = new ArrayList<InetAddress>();

		for(PcapNetworkInterface i : Pcaps.findAllDevs()){
			for(PcapAddress x : i.getAddresses()){
				InetAddress xAddr = x.getAddress();
				if(xAddr != null && x.getNetmask() != null && xAddr.getAddress().length == 4 && !xAddr.toString().equals("/0.0.0.0")){
					NetworkInterface inf = NetworkInterface.getByInetAddress(x.getAddress());
					if(inf != null && inf.isUp() && !inf.isVirtual()){
						inets.add(xAddr);
						lanIP.addItem((lanIP.getItemCount() + 1) + " - " + inf.getDisplayName() + " ::: " + xAddr.getHostAddress());
						System.out.println("Found: " + lanIP.getItemCount() +" - "+ inf.getDisplayName() + " ::: " + xAddr.getHostAddress());
					}
				}
			}
		}

		if(lanIP.getItemCount() == 0){
			JOptionPane.showMessageDialog(null, "Unable to locate devices.\nPlease try running the program in Admin Mode.\nIf this does not work, you may need to reboot your computer.", 
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		final JButton start = new JButton("Start");
		start.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					if(lanText.getText().length() >= 7 && !lanText.getText().equals("0.0.0.0")){ // 7 is because the minimum field is 0.0.0.0
						addr = InetAddress.getByName(lanText.getText());
						System.out.println("Using IP from textfield: "+ lanText.getText());
					}else{
						addr = inets.get(lanIP.getSelectedIndex());
						System.out.println("Using device from dropdown: "+ lanIP.getSelectedItem());
					}
					Settings.set("addr", addr.getHostAddress().replaceAll("/", ""));
					frame.setVisible(false);
					frame.dispose();
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
			}
		});

		frame.setLayout(new GridLayout(5,1));
		frame.add(ipLab);
		frame.add(lanIP);
		frame.add(lanLabel);
		frame.add(lanText);
		frame.add(start);
		frame.setAlwaysOnTop(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		while(frame.isVisible()){
			Thread.sleep(10);
		}
	}
}
