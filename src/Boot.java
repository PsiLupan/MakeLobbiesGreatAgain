import java.io.IOException;
import java.net.InetAddress;
import org.pcap4j.core.NotOpenException;
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
	public static void main(String[] args){
		try {
			InetAddress addr = InetAddress.getByName("192.168.1.84"); //TODO: Add user input of IP, since device enum not human readable
			PcapNetworkInterface nif = Pcaps.getDevByAddress(addr);

			int snapLen = 65536;
			PromiscuousMode mode = PromiscuousMode.PROMISCUOUS;
			int timeout = 0;
			PcapHandle handle = nif.openLive(snapLen, mode, timeout);

			short pckCount = 0;
			long lastPacketTime = 0;
			String currSrv = null;

			while(true){
				Packet packet = handle.getNextPacket();
				
				if(packet != null){
					IpV4Packet ippacket = packet.get(IpV4Packet.class);
					
					if(ippacket != null){
						if(ippacket.getHeader().getProtocol() == IpNumber.UDP && !ippacket.getHeader().getSrcAddr().isSiteLocalAddress()){
							UdpPacket udppack = ippacket.get(UdpPacket.class);
							String srcAddrStr = ippacket.getHeader().getSrcAddr().toString(); // Shows as '/0.0.0.0'

							if(udppack.getPayload().getRawData().length == 4){ //Leave/Join packet is always payload length 4
								if(lastPacketTime == 0 || System.currentTimeMillis() - lastPacketTime < 10000){
									pckCount++;
									lastPacketTime = System.currentTimeMillis();

									if(pckCount == 3){ //3 of the leave/join packet are always sent in rapid succession
										if(!srcAddrStr.equals(currSrv)){ //TODO: Bugfix - Joining same killer twice won't trigger a lookup
											Thread t = new Thread(new Geolocate(srcAddrStr));
											t.start();
											currSrv = srcAddrStr; //This serves to prevent seeing the message upon joining then leaving
										}
										pckCount = 0;
									}
								}else{ //If the packets take more than 10 secs, probably wrong packet
									lastPacketTime = 0;
									pckCount = 0;
								}
							}
						}
					}
				}
			}
		} catch (PcapNativeException | NotOpenException | IOException e) {
			e.printStackTrace();
		}

	}
}
