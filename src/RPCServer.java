/**
 * RPCServer.java is the background RPC thread that handles
 * all the incoming RPC requests.
 */
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class RPCServer extends Thread {
	
	RPCServer() {
	}
	
	public void run() {
		try {
			//Start the RPC server on the random port
			DatagramSocket server = new DatagramSocket();
			
			byte[] data = new byte[512];
			while(true) {
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);
				Project1.port_udp = server.getLocalPort();
				//Wait for the incoming RPC Client request
				server.receive(receivePacket);
				
				String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
				//Get opcode
				int opcode = Integer.parseInt(packetList[1]);
				//Get IP Address from the incoming packet
				InetAddress ipaddr = receivePacket.getAddress();
				//Add this incoming packet's address to the mbrSet
				if( !(InetAddress.getLocalHost().getHostAddress().replace("/", "") + ":" + Project1.port_udp).equals(ipaddr.toString().replace("/", "") + ":" + packetList[packetList.length - 1]) && Project1.mbrSet.indexOf(ipaddr.toString().replace("/", "") + ":" + packetList[packetList.length - 1]) == -1 ) {
					Project1.mbrSet.add(ipaddr.toString().replace("/", "") + ":" + packetList[packetList.length - 1]);
				}
				
				String packet = "";
				byte[] output = null;
				SessionValue sv = new SessionValue();				
				
				switch(opcode) {
					case 1: 
						/**
						 * Session Read case
						 */
						sv = sessionRead(packetList[2], Integer.parseInt(packetList[3]));
						if (sv != null) {
							packet = packetList[0] + "#" + packetList[2] + "#" + sv.message + "#" + sv.version_number + "#" + sv.time_stamp;
						} else 
							packet = packetList[0] + "#" + packetList[2] + "#" + "NAK";
						 
						break;
					
					case 2:
						/**
						 * Session Write/Update case
						 */
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
						/**
						 * Session Delete case
						 */
						if(sessionDelete(packetList[2], Integer.parseInt(packetList[3]))){
							packet = packetList[0] + "#" + "ACK";
						} else {
							packet = packetList[0] + "#" + "NAK";
						}
						break;
					
					case 4:
						/**
						 * Accelerated membership protocol
						 */
						ArrayList<String> members = getMembers(Integer.parseInt(packetList[2]));
						packet = packetList[0];
						for(String s : members) {
							packet += "#" + s;
						}
						break;
				}
				//Send the output packet
				output = packet.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(output, output.length,ipaddr,receivePacket.getPort());
				server.send(sendPacket);
			}
		} catch(Exception e) {
			
		}
	}
	
	/**
	 * SessionRead function reads session table data and replies back with session data
	 * @param SID Session ID to read the session
	 * @param version version number passed by the incoming request
	 * @return SessionTable entry if found or null otherwise
	 */
	SessionValue sessionRead(String SID, int version) {
		//Find appropriate session table entry
		if (Project1.sessionTable.containsKey(SID)) {
			//Compare versions, if the version in the table is greater than the incoming version then send the data
			if (Integer.parseInt(Project1.sessionTable.get(SID).version_number) >= version) {
				return Project1.sessionTable.get(SID);
			}
		}
		return null;
	}
	
	/**
	 * SessionWrite function writes/updates session table entry
	 * @param SID Session ID to write
	 * @param sv SessionValue object
	 * @return whether write request succeeded or not
	 */
	private boolean sessionWrite(String SID, SessionValue sv) {
		//Update case
		if (Project1.sessionTable.containsKey(SID)) {
			//If the incoming version number is greater than stored version then only proceed further
			if (Integer.parseInt(Project1.sessionTable.get(SID).version_number) <= Integer.parseInt(sv.version_number)) {
				Project1.sessionTable.get(SID).message = sv.message;
				Project1.sessionTable.get(SID).time_stamp = sv.time_stamp;
				Project1.sessionTable.get(SID).version_number = sv.version_number;
				return true;
			}
		} else {
			//Write case
			Project1.sessionTable.put(SID, sv);
			return true;
		}
		return false;
	}
	
	/**
	 * SessionDelete function to delete session entry from the table
	 * @param SID session id
	 * @param version version number
	 * @return whether session delete request succeeded or not
	 */
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
	
	/**
	 * GetMembers function to return memberset of one server to the other
	 * @param no Howmany entries of the mbrSet are to be transferred
	 * @return List of the IPPs
	 */
	private ArrayList<String> getMembers(int no) {
		//If number of the members requested are greater than size of the mbrSet then return all the members
		if(Project1.mbrSet.size() < no) {
			return Project1.mbrSet;
		}
		//Otherwise transfer the subset of the mbrSet
		ArrayList<String> members = new ArrayList<String>();
		for(int i = 0; i < no; i++){
			int index = (int) Math.random() * Project1.mbrSet.size();
			members.add(Project1.mbrSet.get(index));
		}
		return members;
	}
}