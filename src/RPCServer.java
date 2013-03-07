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
				server.receive(receivePacket);
				
				Project1.port_udp = server.getPort();
				
				String opcode = new String(receivePacket.getData());
				InetAddress ipaddr = receivePacket.getAddress();
				int portNo = receivePacket.getPort();
				
				if( Project1.mbrSet.indexOf(ipaddr.toString() + ":" + portNo) != -1 ) {
					Project1.mbrSet.add(ipaddr.toString() + ":" + portNo);
				}
				
				byte[] output = new byte[512];
				SessionValue sv = new SessionValue();
				
				switch(Integer.parseInt(opcode)) {
					case 1: sv = sessionRead(SID, version);
					break;
					
					case 2: sessionWrite(SID, sv);
					break;
					
					case 3: sessionDelete(SID, version)
					break;
					
					case 4: getMembers (no)
					break;
				}
				
				DatagramPacket sendPacket = new DatagramPacket(output, output.length,
						ipaddr,portNo);
				server.send(sendPacket);
			}
		} catch(Exception e) {
			
		}
	}
	
	private SessionValue sessionRead(String SID, int version) {
		for (Iterator<Map.Entry<String, SessionValue>> itr = Project1.sessionTable.entrySet().iterator(); itr.hasNext(); ) {
			Map.Entry<String, SessionValue> entry = itr.next();

		    if (SID.equals(entry.getKey()) && Integer.parseInt(entry.getValue().version_number) >= version) {
		    	return entry.getValue();
		    }
		}
		return null;
	}
	
	private void sessionWrite(String SID, SessionValue sv) {
		for (Iterator<Map.Entry<String, SessionValue>> itr = Project1.sessionTable.entrySet().iterator(); itr.hasNext(); ) {
			Map.Entry<String, SessionValue> entry = itr.next();

		    if (SID.equals(entry.getKey()) && Integer.parseInt(entry.getValue().version_number) < Integer.parseInt(sv.version_number)) {
		    	entry.getValue().message = sv.message;
		    	entry.getValue().time_stamp = sv.time_stamp;
		    	entry.getValue().version_number = sv.version_number;
		    }
		}
	}
	
	private void sessionDelete(String SID, int version) {
		for (Iterator<Map.Entry<String, SessionValue>> itr = Project1.sessionTable.entrySet().iterator(); itr.hasNext(); ) {
		    Map.Entry<String, SessionValue> entry = itr.next();

		    if (SID.equals(entry.getKey()) && Integer.parseInt(entry.getValue().version_number) <= version) {
		        itr.remove();
		    }
		}
	}
	
	private ArrayList<String> getMembers(int no) {
		if(Project1.mbrSet.size() < no) {
			return Project1.mbrSet;
		}
		ArrayList<String> members = new ArrayList<>();
		for(int i = 0; i < no; i++){
			int index = (int) Math.random() * Project1.mbrSet.size();
			members.add(Project1.mbrSet.get(index));
		}
		return members;
	}
}
