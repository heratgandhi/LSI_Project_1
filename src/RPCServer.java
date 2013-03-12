import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class RPCServer extends Thread {
	
	RPCServer() {
	}
	
	public void run() {
		try {
			DatagramSocket server = new DatagramSocket();
			
			
			byte[] data = new byte[512];
			while(true) {
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);
				Project1.port_udp = server.getLocalPort();
				
				server.receive(receivePacket);
				
				String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
				int opcode = Integer.parseInt(packetList[1]);
				
				InetAddress ipaddr = receivePacket.getAddress();
				
				if( !(InetAddress.getLocalHost().getHostAddress() + ":" + Project1.port_udp).equals(ipaddr.toString() + ":" + packetList[packetList.length - 1]) && Project1.mbrSet.indexOf(ipaddr.toString() + ":" + packetList[packetList.length - 1]) == -1 ) {
					Project1.mbrSet.add(ipaddr.toString() + ":" + packetList[packetList.length - 1]);
				}
				
				String packet = "";
				byte[] output = null;
				SessionValue sv = new SessionValue();				
				
				//System.out.println(packetList[2]+" "+Integer.parseInt(packetList[3]));
				
				switch(opcode) {
					case 1: 
						sv = sessionRead(packetList[2], Integer.parseInt(packetList[3]));
						if (sv != null) {
							packet = packetList[0] + "#" + packetList[2] + "#" + sv.message + "#" + sv.version_number + "#" + sv.time_stamp;
						} else 
							packet = packetList[0] + "#" + packetList[2] + "#" + "NAK";
						 
						break;
					
					case 2:
						sv.message = packetList[3];
						sv.version_number = packetList[4];
						sv.time_stamp = Long.parseLong(packetList[5]);
						if(sessionWrite(packetList[2], sv)) {
							packet = packetList[0] + "#" + "ACK";
						} else {
							packet = packetList[0] + "#" + "NAK";
						}
						break;
					
					case 3:
						if(sessionDelete(packetList[2], Integer.parseInt(packetList[3]))){
							packet = packetList[0] + "#" + "ACK";
						} else {
							packet = packetList[0] + "#" + "NAK";
						}
						break;
					
					case 4: 
						ArrayList<String> members = getMembers(Integer.parseInt(packetList[2]));
						packet = packetList[0];
						for(String s : members) {
							packet += "#" + s;
						}
						break;
				}
				
				output = packet.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(output, output.length,ipaddr,receivePacket.getPort());
				server.send(sendPacket);
			}
		} catch(Exception e) {
			
		}
	}
	
	SessionValue sessionRead(String SID, int version) {
		if (Project1.sessionTable.containsKey(SID)) {
			if (Integer.parseInt(Project1.sessionTable.get(SID).version_number) >= version) {
				return Project1.sessionTable.get(SID);
			}
		}
		return null;
	}
	
	private boolean sessionWrite(String SID, SessionValue sv) {
		if (Project1.sessionTable.containsKey(SID)) {
			if (Integer.parseInt(Project1.sessionTable.get(SID).version_number) <= Integer.parseInt(sv.version_number)) {
				Project1.sessionTable.get(SID).message = sv.message;
				Project1.sessionTable.get(SID).time_stamp = sv.time_stamp;
				Project1.sessionTable.get(SID).version_number = sv.version_number;
				return true;
			}
		} else {
			Project1.sessionTable.put(SID, sv);
			return true;
		}
		return false;
	}
	
	private boolean sessionDelete(String SID, int version) {
		for (Iterator<Map.Entry<String, SessionValue>> itr = Project1.sessionTable.entrySet().iterator(); itr.hasNext(); ) {
		    Map.Entry<String, SessionValue> entry = itr.next();

		    if (SID.equals(entry.getKey()) && Integer.parseInt(entry.getValue().version_number) <= version) {
		        itr.remove();
		        return true;
		    }
		}
		return false;
	}
	
	private ArrayList<String> getMembers(int no) {
		if(Project1.mbrSet.size() < no) {
			return Project1.mbrSet;
		}
		ArrayList<String> members = new ArrayList<String>();
		for(int i = 0; i < no; i++){
			int index = (int) Math.random() * Project1.mbrSet.size();
			members.add(Project1.mbrSet.get(index));
		}
		return members;
	}
}