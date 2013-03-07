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
				String opcode = new String(receivePacket.getData());
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
						receivePacket.getAddress(),receivePacket.getPort());
				server.send(sendPacket);
			}
		} catch(Exception e) {
			
		}
	}
}
