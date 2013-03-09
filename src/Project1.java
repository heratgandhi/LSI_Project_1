/**
 * Authors: Suman Somasundar, Herat Gandhi, Vinayaka Dattatraya
 * Last modified date: 26th February, 2013
 * Project1.java servlet is the file which is first accessed
 * by the client. All the cookie and session related logic
 * is implemented in this file.
 * 
 *  Note: As hashtable is thread safe we have not explicitely
 *  used synchronized in our code.
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SessionValue class is used to store session related values in
 * the Hash Table. The key in the Hash Table is session id and 
 * the value is object of type SessionValue.
 */
class SessionValue {
	String version_number; //Version number of data
	long time_stamp; //Time Stamp value
	String message; //Message
}

/**
 * Servlet implementation class Project1
 * This class handles all the user interaction using doGet method.
 */
public class Project1 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final int minutes = 1;
	
	//Hash Table sessionTable is used to store session data
	public static ConcurrentHashMap<String,SessionValue> sessionTable = new ConcurrentHashMap<String,SessionValue>(100);
	
	public static ArrayList<String> mbrSet = new ArrayList<String>();
	
	public static int port_udp;
	
	/**
     * Default constructor. 
     */
    public Project1() {
        // TODO Auto-generated constructor stub
    }
    
    public void init() {
    	RPCServer rpcServerT = new RPCServer();
    	rpcServerT.start();
    }
    
    /**
     * RemoveCookie method
     * @param key Specific session id to be removed from the session table
     * This method iterates through the session table when it finds
     * the session id specified by the key then it removes that session entry 
     * from the session table.
     */
    void RemoveCookie(String key) {
    	for (Iterator<Map.Entry<String, SessionValue>> itr = sessionTable.entrySet().iterator(); itr.hasNext(); ) {
		    Map.Entry<String, SessionValue> entry = itr.next();

		    if (key.equals(entry.getKey())) {
		        itr.remove();
		    }
		}
	}
    
    /**
     * RemoveExpiredCookie method
     * This method iterates through the session table when it finds
     * the stale session entry, it removes that session entry 
     * from the session table.
     */
    void RemoveExpiredCookie() {
    	for (Iterator<Map.Entry<String, SessionValue>> itr = sessionTable.entrySet().iterator(); itr.hasNext(); ) {
		    Map.Entry<String, SessionValue> entry = itr.next();
		    /*
		     * Check whether current timestamp is greater than session's
		     * expiration time, if yes then remove that entry from session
		     * table.
		     */
		    if (entry.getValue().time_stamp < new Date().getTime()) {
		        itr.remove();
		    }
		}
    }
    
    String RPCClientStub(int opcode, String sessionid, SessionValue sv, String ipp1, String ipp2) {
		try {		    
			switch(opcode) {
				case 1:
					byte[] outBuf;
					byte[] inBuf = new byte[512];
					int call_id = (int)(Math.random() * 1000);
					String packetS = call_id + "#" + opcode + "#" + sessionid + "#" + sv.message + "#" + sv.version_number + "#" + sv.time_stamp;
					outBuf = packetS.getBytes();
					int randomNode = (int)(Math.random() * mbrSet.size());
					String ipp = mbrSet.get(randomNode);
					InetAddress ipA = InetAddress.getByName(ipp.substring(0,ipp.indexOf(':')));
					int portA = Integer.parseInt(ipp.substring(ipp.indexOf(':')+1));
					//Send to randomNode
					try {
						DatagramSocket clientSocket = new DatagramSocket();
						DatagramPacket sendPacket = new DatagramPacket(outBuf, outBuf.length, ipA, portA);
					    clientSocket.send(sendPacket);
					    
					    clientSocket.setSoTimeout(1*60*1000);
					    
					    DatagramPacket receivePacket = new DatagramPacket(inBuf, inBuf.length);
					    clientSocket.receive(receivePacket);				    
					} catch(Exception e) {
						mbrSet.remove(ipp);
					}
				break;
				case 2:
				break;
				case 3:
				break;
				case 4:
					byte[] outBuf4;
					byte[] inBuf4 = new byte[512];
					int call_id4 = (int)(Math.random() * 1000);
					String packetS4 = call_id4 + "#" + opcode + "#" + sessionid;
					outBuf = packetS4.getBytes();
					
					InetAddress ipA4 = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
					int portA4 = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
					
					InetAddress ipA42 = null;
					int portA42 = 0;
					if(ipp2 != "") {
						ipA42 = InetAddress.getByName(ipp2.substring(0,ipp1.indexOf(':')));
						portA42 = Integer.parseInt(ipp2.substring(ipp1.indexOf(':')+1));
					}
					
					try {
						DatagramSocket clientSocket = new DatagramSocket();
						DatagramPacket sendPacket = new DatagramPacket(outBuf, outBuf.length, ipA4, portA4);
					    clientSocket.send(sendPacket);
					    
					    clientSocket.setSoTimeout(1*60*1000);
					    
					    DatagramPacket receivePacket = new DatagramPacket(inBuf4, inBuf4.length);
					    clientSocket.receive(receivePacket);				    
					} catch(Exception e) {
						mbrSet.remove(ipp1);
					}
					if(ipp2 != "") {
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf, outBuf.length, ipA42, portA42);
						    clientSocket.send(sendPacket);
						    
						    clientSocket.setSoTimeout(1*60*1000);
						    
						    DatagramPacket receivePacket = new DatagramPacket(inBuf4, inBuf4.length);
						    clientSocket.receive(receivePacket);				    
						} catch(Exception e) {
							mbrSet.remove(ipp2);
						}
					}
				break;
			}
		} catch(Exception ioe) {
		}
		return "";
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");	
		Cookie c[] = request.getCookies(); //Get cookies from the request
		String msg = "";
		String msg1;
		String session_id_c;
		
		/*
		 * If the current request is first request from the client then
		 * it won't have any cookie. If client's cookie is expired then 
		 * also client's request won't have any cookie. In both these cases
		 * we will have to start the new session otherwise we do not start
		 * the new session. 
		 */
		if(c == null) {
			String session_id = UUID.randomUUID().toString().replaceAll("-", "").substring(0,5); //Generate unique session id using UUID class
			String version_no = "1"; //Set initial version number to 1
			String location_data = "";
			String backup_n;
			
			location_data = InetAddress.getLocalHost().getHostAddress() + ":" + port_udp;
						
			//Create corresponding entry in the session table
			SessionValue sv = new SessionValue();
			sv.message = msg;
			sv.version_number = version_no;
			sv.time_stamp = new Date().getTime() + (minutes * 60 * 1000);
			
			sessionTable.put(session_id, sv);
			
			if(mbrSet.size() != 0) {
				backup_n = RPCClientStub(1,session_id,sv,"","");
				location_data += "@" + backup_n;
			}
			
			msg = "Welcome for the first time..."; //Default message
			
			String message_var = msg;
			
			//Create cookie value
			String message = session_id + "#" + version_no + "#" + message_var + "@" + location_data; 
			
			Cookie ck = new Cookie("CS5300PROJ1SESSIONSVH",message);
			//Currently session timeout period is of 1 minute.
			ck.setMaxAge(60);
			//Send cookie to the client
			response.addCookie(ck);
						
			//Remove any expired session entry from the session table
			RemoveExpiredCookie();
		}
		/*
		 * If client's request has some cookie with it then process the cookie based on various events like
		 * refresh, replace and logout.
		 */		
		else if(c != null) {
			
			//Iterate through all cookies and find the cookie for our application
			for (int i=0;i<c.length;i++) {
				if(c[i].getName().equals("CS5300PROJ1SESSIONSVH")) {
					//Get session id from cookie
					session_id_c = c[i].getValue().substring(0,c[i].getValue().indexOf("#"));
					
					//Get session value corresponding to the above session id
					//### If session data is not available here then go and fetch from other servers
					SessionValue sv1 = (SessionValue) sessionTable.get(session_id_c);
					
					//Get message value from cookie
					msg = c[i].getValue().substring(c[i].getValue().lastIndexOf("#")+1,c[i].getValue().indexOf("@"));
					
					String ipp_tpl = c[i].getValue().substring(c[i].getValue().indexOf("@")+1);
					String ipp1 = "";
					String ipp2 = "";
					ipp1 = ipp_tpl;
					
					//There is not another ipp in the cookie
					if(ipp_tpl.indexOf("@") != -1) {
						ipp1 = ipp_tpl.substring(0,ipp_tpl.indexOf("@"));
						ipp2 = ipp_tpl.substring(ipp_tpl.indexOf("@")+1);
					} 
					
					//If command is replace and new string is not empty string then
					if(request.getParameter("replace") != null 
							&& !request.getParameter("replace").trim().equals("")) {
						
						//Get string value with which we want to replace the current message
						msg1 = request.getParameter("replace");
						c[i].setValue(c[i].getValue().substring( 0, c[i].getValue().lastIndexOf("#")+1) + msg1 + 
								c[i].getValue().substring(c[i].getValue().lastIndexOf("@")));
						c[i].setMaxAge(60);
						//Send updated cookie to the client
						response.addCookie(c[i]);
						
						//Replace the message in the session table
						sv1.message = msg1;
					}
					//If command is replace and new string is empty then treat this as refresh
					if(request.getParameter("replace") != null && 
							request.getParameter("replace").trim().equals("")) {
						//Update cookie expiration value
						c[i].setMaxAge(60);
						//Send new cookie
						response.addCookie(c[i]);
					}
					//If command is logout then
					if(request.getParameter("cmd") != null){
						if(request.getParameter("cmd").equals("logout")) {
							c[i].setMaxAge(0); //Set cookie expiration time to 0
							response.addCookie(c[i]); //Send new cookie
							
							//Remove
							RPCClientStub(4,session_id_c,sv1,ipp1,ipp2);
							
							RemoveCookie(session_id_c); //Remove entry from session table
														
							response.sendRedirect(request.getContextPath() + "/Project.jsp?cmd=logout"); //Display logout page
						}
						//If command is refresh then
						if(request.getParameter("cmd").equals("refresh")) {
							c[i].setMaxAge(60); //Update cookie expiration time
							response.addCookie(c[i]); //Send new cookie
						}
				}
				//Update entry in the session table	
				sv1.time_stamp = new Date().getTime() + (minutes * 60 * 1000); 
				
				int vno = Integer.parseInt(sv1.version_number);
				
				sv1.version_number = (++vno) + ""; //Increment version number
					
				}
			}
		}
		if(request.getParameter("cmd") != null && request.getParameter("cmd").equals("logout")){
			
		} else {
			response.sendRedirect(request.getContextPath() + "/Project.jsp"); //Redirect to the jsp file
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
