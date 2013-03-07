import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class RPCServer extends Thread {
	
	RPCServer() {
	}
	
	public void run() {
		try {
			DatagramSocket server = new DatagramSocket();
			byte[] data = new byte[512];
			while(true) {
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);
				server.receive(receivePacket);
				
				Project1.port_udp = server.getPort();
				
				String opcode = new String(receivePacket.getData());
				InetAddress ipaddr = receivePacket.getAddress();
				int portNo = receivePacket.getPort();
				
				if( Project1.mbrSet.indexOf(ipaddr.toString() + ":" + portNo) != -1 ) {
					Project1.mbrSet.add(ipaddr.toString() + ":" + portNo);
				}
				
				byte[] output = new byte[512];
				
				switch(Integer.parseInt(opcode)) {
					case 1:
					break;
					
					case 2:
					break;
					
					case 3:
					break;
					
					case 4:
					break;
				}
				
				DatagramPacket sendPacket = new DatagramPacket(output, output.length,
						ipaddr,portNo);
				server.send(sendPacket);
			}
		} catch(Exception e) {
			
		}
	}
}
