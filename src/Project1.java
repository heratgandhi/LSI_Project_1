/**
 * Authors: Suman Somasundar, Herat Gandhi, Vinayaka Dattatraya
 * Project1.java servlet is the file which is first accessed
 * by the client. All the cookie and session related logic
 * is implemented in this file.
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.StopInstancesRequest;*/

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
 * Cleanup Process is a thread used to delete expired session entries from
 * session table. This thread will get executed by each server at a particular
 * interval which we have currently set at 5 minutes. 
 */
class CleanUpProcess extends TimerTask {
    public void run() {
    	/**
         * RemoveExpiredCookie method
         * This method iterates through the session table when it finds
         * the stale session entry, it removes that session entry 
         * from the session table.
         */
    	for (Iterator<Map.Entry<String, SessionValue>> itr = Project1.sessionTable.entrySet().iterator(); itr.hasNext(); ) {
    		/**
    		 * We first lock the row which we may delete later. So that it offers better synchronization. 
    		 */
        	synchronized (itr) {
        		Map.Entry<String, SessionValue> entry = itr.next();
    		    /**
    		     * Check whether current timestamp is greater than session's
    		     * expiration time, if yes then remove that entry from session
    		     * table.
    		     */
    		    if (entry.getValue().time_stamp < new Date().getTime()) {
    		    	itr.remove();    			    
    			}
			}		            	
    	}
    }
  }

/**
 * Servlet implementation class Project1
 * This class handles all the user interaction using doGet method.
 */
public class Project1 extends HttpServlet implements ServletContextListener {
	private static final long serialVersionUID = 1L;
	public static final int minutes = 5;
	public static final int wait_time_seconds = 100;
	private static final int delta = 5;
	private static final int tau = 5;
	
	//set resilience
	private static int k = 1;
	private int resilience = k;
	
	//Concurrent Hashmap sessionTable is used to store session data
	public static ConcurrentHashMap<String,SessionValue> sessionTable = new ConcurrentHashMap<String,SessionValue>(100);
	public static ArrayList<String> mbrSet = new ArrayList<String>();
	
	public static int port_udp;
	public static int session_loc;
	
	Timer timer;
	
	/**
     * Default constructor. 
     */
    public Project1() {
        // TODO Auto-generated constructor stub
    	/**
    	 * Start the periodic cleanup thread.
    	 */
    	timer = new Timer();
    	timer.scheduleAtFixedRate(new CleanUpProcess(), 0, 5 * 60 * 1000);
    }
    
    public void init(ServletConfig config) {
    	try {
    		super.init(config);
    		Thread.sleep(100);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    private ExecutorService executor;
    
    /**
     * Start the RPC thread for the server which listens to the incoming RPC reuqests on the server
     */
    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(new RPCServer());
    }

    public void contextDestroyed(ServletContextEvent event) {
        executor.shutdown();
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
     * RPC Client Stub method
     * @param opcode Operation code for the currrent operation
     * @param sessionid Session id of the current session
     * @param sv SessionValue object related to the current session
     * @param ipp1 IPP Primary
     * @param ipp2 IPP Backup
     * @param version Version number
     * @return Data or null if operation failed
     */
    String RPCClientStub(int opcode, String sessionid, SessionValue sv, String ipp1, String ipp2, String version) {
		try {		    
			switch(opcode) {
				case 1:
					/**
					 * Session Read case
					 * Read session from IPP Primary if that failes then read session from IPP Backup
					 */
					byte[] outBuf_r;
					byte[] inBuf_r = new byte[512];
					byte[] inBuf_r1 = new byte[512];
					//Generate unique call id for this operation
					int call_id_r = (int)(Math.random() * 1000);
					//Create packet
					String packetS_r = call_id_r + "#" + opcode + "#" + sessionid + "#" + version.trim() + "#" + port_udp;
					outBuf_r = packetS_r.getBytes();
					
					InetAddress ipRead = null;
					int portRead;
					
					for(int i = 0; i < ipp.size(); i++)
					{
						ipRead = InetAddress.getByName(ipp.get(i).substring(0,ipp.get(i).indexOf(':')));
						portRead = Integer.parseInt(ipp.get(i).substring(ipp.get(i).indexOf(':')+1));
						
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_r, outBuf_r.length, ipRead, portRead);
						    clientSocket.send(sendPacket);
						    
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    
						    DatagramPacket receivePacket = new DatagramPacket(inBuf_r, inBuf_r.length);
						    clientSocket.receive(receivePacket);
						    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
						    int callID = Integer.parseInt(packetList[0]);
						    //Compare call id
						    if(callID == call_id_r) {
						    	session_loc = 1;
						    	return new String(inBuf_r,0,receivePacket.getLength());
						    }
						} catch(Exception e) {
							mbrSet.remove(ipp.get(i));
						}
					}
					return null;
					
				case 2:
					/**
					 * Session Write/Update case
					 * Current server serving request will become primary server and it will
					 * find one backup server from the mbrSet.
					 */
					byte[] outBuf_u;
					byte[] inBuf_u = new byte[512];
					//Generate unique call id for this operation
					int call_id_u = (int)(Math.random() * 1000);
					String packetS4_u = call_id_u + "#" + opcode + "#" + sessionid + "#" + sv.message + "#" + sv.version_number + "#" + sv.time_stamp + "#" + port_udp;
					outBuf_u = packetS4_u.getBytes();
					String backUP = "";
					String ack = "NAK"; 
					ArrayList<String> dupMbrSet = mbrSet;
					
					InetAddress ipWrite = null;
					int portWrite;
					for(int i = 0; i < ipp.size(); i++){
						ack = "NAK";
						ipWrite = InetAddress.getByName(ipp.get(i).substring(0,ipp.get(i).indexOf(':')));
						portWrite = Integer.parseInt(ipp.get(i).substring(ipp.get(i).indexOf(':')+1));
						dupMbrSet.remove(ipp.get(i));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipWrite, portWrite);
						    clientSocket.send(sendPacket);
						    //Wait for the acknowledgement
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    //Get ack
						    DatagramPacket receivePacket = new DatagramPacket(inBuf_u, inBuf_u.length);
						    clientSocket.receive(receivePacket);
						    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
						    int callID = Integer.parseInt(packetList[0]);
						    //Check for the call id
						    if(callID == call_id_u) {
						    	if(packetList[1].equals("ACK")){
						    		backUP += "#" + ipp.get(i);
						    		resilience--;
						    	}
						    }
						} catch(Exception e) {
							mbrSet.remove(ipp.get(i));
							ack = "NAK";
						}
						if(resilience == 0){
							return backUP;
						}
					} 
					
					while(k != 0 && dupMbrSet.size() > 0){
						int randomNode;
						InetAddress ipWrite1;
						int portWrite1;
						randomNode = (int)(Math.random() * dupMbrSet.size());
						String ippWrite = dupMbrSet.get(randomNode);
						ipWrite1 = InetAddress.getByName(ippWrite.substring(0,ippWrite.indexOf(':')));
						portWrite1 = Integer.parseInt(ippWrite.substring(ipp.indexOf(':')+1));
						dupMbrSet.remove(ippWrite);
						try{
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipWrite1, portWrite1);
						    clientSocket.send(sendPacket);
						    //Wait for the acknowledgement
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    //Get data from other side
						    DatagramPacket receivePacket = new DatagramPacket(inBuf_u, inBuf_u.length);
						    clientSocket.receive(receivePacket);
						    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
						    int callID = Integer.parseInt(packetList[0]);
						    //Check for the call id
						    if(callID == call_id_u) {
						    	ack = packetList[1];
						    	//Check whether operation succeeded or not
						    	if(ack.equals("ACK")){
						    		backUP += "#" + ippWrite;
						    		resilience--;
						    	}
						    }
						} catch (Exception e) {
							mbrSet.remove(ipp);
						}
					}					
				break;
				case 3:
					/**
					 * Session Delete case
					 * Remove session table entry from servers mentioned in the cookie.
					 */
					byte[] outBuf4;
					byte[] inBuf4 = new byte[512];
					//Generate unique call numbers
					int call_id4 = (int)(Math.random() * 1000);
					String packetS4 = call_id4 + "#" + opcode + "#" + sessionid + "#" + version + "#" + port_udp;
					outBuf4 = packetS4.getBytes();
					InetAddress ipA4;
					int portA4;
					for (int i = 0; i < ipp.size(); i++) {
						ipA4 = InetAddress.getByName(ipp.get(i).substring(0,ipp.get(i).indexOf(':')));
						portA4 = Integer.parseInt(ipp.get(i).substring(ipp.get(i).indexOf(':')+1));
						
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf4, outBuf4.length, ipA4, portA4);
							//Send delete request
						    clientSocket.send(sendPacket);
						    //Wait for the ack
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    //Get data
						    DatagramPacket receivePacket = new DatagramPacket(inBuf4, inBuf4.length);
						    clientSocket.receive(receivePacket);				    
						} catch(Exception e) {
							mbrSet.remove(ipp.get(i));
						}
					}
				break;
				case 4:
					/**
					 * Accelerated Group membership protocol
					 * Get whole/subset of mbrSet of the server found in the cookie
					 */
					byte[] outBuf5;
					byte[] inBuf5 = new byte[512];
					int call_id5 = (int)(Math.random() * 1000);
					String packetS5 = call_id5 + "#" + opcode + "#" + version + "#" + port_udp;
					outBuf5 = packetS5.getBytes();
					ipA4 = InetAddress.getByName(ipp.get(0).substring(0,ipp.get(0).indexOf(':')));
					portA4 = Integer.parseInt(ipp.get(0).substring(ipp.get(0).indexOf(':')+1));
					
					try {
						DatagramSocket clientSocket = new DatagramSocket();
						DatagramPacket sendPacket = new DatagramPacket(outBuf5, outBuf5.length, ipA4, portA4);
						//Send the packet
					    clientSocket.send(sendPacket);
					    //Wait for the data
					    clientSocket.setSoTimeout(wait_time_seconds * 1000);
					    //Receive data
					    DatagramPacket receivePacket = new DatagramPacket(inBuf5, inBuf5.length);
					    clientSocket.receive(receivePacket);	
					    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
					    if(Integer.parseInt(packetList[0]) == call_id5) {
					    	for(int i = 1; i<packetList.length; i++) {
					    		if( !packetList[i].replace("/", "").equals(InetAddress.getLocalHost().getHostAddress().replace("/", "") + ":" + port_udp)
					    			&&	
					    			mbrSet.indexOf(packetList[i].replace("/", "")) == -1	) {
					    			mbrSet.add(packetList[i].replace("/", ""));
					    		}
					    	}
					    }
					} catch(Exception e) {

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
		//Get IPP Local
		String ippLocal = InetAddress.getLocalHost().getHostAddress().replace("/", "") + ":" + port_udp;
		boolean redirect = false;
		
		System.out.println("Request Arrived!");
		/**
		 * Stop amazon ec2 instance using Amazon SDK
		 */
		if(request.getParameter("cmd") != null && request.getParameter("cmd").equals("error")) {
			/*BasicAWSCredentials awsCredentials = new BasicAWSCredentials("AKIAJ22NLENRGJQT4Z2Q", "Y9VFisUjD9NKGntXhjZsGvUQo8cuS4BXgg20FdWL");

			AmazonEC2Client ec2Client = new AmazonEC2Client(awsCredentials);
			ec2Client.setEndpoint("");/*ec2.us-west-1.amazonaws.com

			List<String> instancesToStop = new ArrayList<String>();
	        instancesToStop.add(request.getParameter("instance"));
	        StopInstancesRequest stoptr = new StopInstancesRequest();                
	        stoptr.setInstanceIds(instancesToStop);
	        ec2Client.stopInstances(stoptr);
			return;*/
		}
		
		/*
		 * If the current request is first request from the client then
		 * it won't have any cookie. If client's cookie is expired then 
		 * also client's request won't have any cookie. In both these cases
		 * we will have to start the new session otherwise we do not start
		 * the new session. 
		 */
		if(c == null) {
			String session_id = UUID.randomUUID().toString().replaceAll("-", ""); //Generate unique session id using UUID class
			String version_no = "1"; //Set initial version number to 1
			String backup_n = "";
			
			msg = "Welcome for the first time..."; //Default message
						
			//Create corresponding entry in the session table
			SessionValue sv = new SessionValue();
			sv.message = msg;
			sv.version_number = version_no;
			sv.time_stamp = new Date().getTime() + ((minutes * 60+ 2*delta + tau) * 1000 );
			
			sessionTable.put(session_id, sv);
			//If server's mbrSet is not empty then write the session data at one backup server
			if(mbrSet.size() != 0) {
				backup_n = RPCClientStub(2,session_id,sv,null,"");
			}
			
			//Create cookie value
			String message = session_id + "#" + version_no + "#" + msg + "#" + ippLocal;
			if(backup_n != ""){
				message += "#" + backup_n;
			}
			
			Cookie ck = new Cookie("CS5300PROJ1SESSIONSVH",message);
			//Currently session timeout period is of 1 minute.
			ck.setMaxAge(minutes * 60 + delta);
			//Send cookie to the client
			response.addCookie(ck);
						
			session_loc = 0;
			System.out.println("Default");
		}
		/*
		 * If client's request has some cookie with it then process the cookie based on various events like
		 * refresh, replace and logout.
		 */		
		else if(c != null) {
			
			boolean action = false;
			//Iterate through all cookies and find the cookie for our application
			for (int i=0;i<c.length;i++) {
				if(c[i].getName().equals("CS5300PROJ1SESSIONSVH")) {
					//Get session id from cookie
					String[] msgIPPStale = c[i].getValue().split("@");
					
					String[] parts = msgIPPStale[0].split("#"); 
					session_id_c = parts[0];
					
					if(msgIPPStale.length > 1){
						String ippStale = msgIPPStale[1];
						if(!ippStale.equals(ippLocal)){
							ArrayList<String> ipStale = new ArrayList<String>();
							ipStale.add(ippStale);
							RPCClientStub(3, session_id_c, null, ipStale, parts[1]);
						}
					}
					
					ArrayList<String> ipp = new ArrayList<String>();
					
					for(int j=3; j<parts.length; j++){
						if(!ippLocal.equals(parts[j])){
						ipp.add(parts[j]);	
							if(mbrSet.indexOf(parts[j]) == -1) {
								mbrSet.add(parts[j].replace("/", ""));
							}
						}
					}
					
					ArrayList<String> ipMbr = new ArrayList<String>();
					if(mbrSet.size() < 2 && ipp.size() > 0 && !ippLocal.equals(ipp.get(0))) {
						ipMbr.add(ipp.get(0));
						RPCClientStub(4, "", null, ipMbr,"4");        //get 4 members from ipp1 
					} else if(mbrSet.size() < 2 && ipp.size() > 1 && !ippLocal.equals(ipp.get(1))) {
						ipMbr.add(ipp.get(1));
						RPCClientStub(4, "", null, ipMbr, "4");
					}
					
					//Get session value corresponding to the above session id
					//If session data is not available here then go and fetch from other servers
					SessionValue sv1 = (SessionValue) sessionTable.get(session_id_c);
					
					if(sv1 == null || (sv1 != null && Integer.parseInt(sv1.version_number) < Integer.parseInt(parts[1]))) {
						String sv1_data = RPCClientStub(1, session_id_c, null, ipp, parts[1] );
						
						if (sv1_data == null) {
							c[i].setMaxAge(0);
							response.addCookie(c[i]);
							
							RemoveCookie(session_id_c);
							
							response.sendRedirect(request.getContextPath() + "/ErrorPage.jsp");
							redirect = true;
						} else {
							sv1 = new SessionValue();
							
							System.out.println("data:" + sv1_data);
							
							String[] parts1 = sv1_data.split("#");
							sv1.message = parts1[2];
							sv1.version_number = parts1[3];
							sv1.time_stamp = Long.parseLong(parts1[4]);
						}
					} else if(sv1 != null && Integer.parseInt(sv1.version_number) >= Integer.parseInt(parts[1])) {
						session_loc = 3;
						System.out.println("From Table");
					}
					
					String new_msg = "";
					//If command is replace and new string is not empty string then
					if(request.getParameter("replace") != null 
							&& !request.getParameter("replace").trim().equals("")) {
						
						//Get string value with which we want to replace the current message
						msg1 = request.getParameter("replace");
						new_msg = msg1;
						action = true;
					}
					

					//If command is replace and new string is empty then treat this as refresh
					if(request.getParameter("replace") != null && 
							request.getParameter("replace").trim().equals("")) {
						new_msg = parts[2];
						action = true;
					}
					
					//If command is refresh then
					if(request.getParameter("cmd") != null && request.getParameter("cmd").equals("refresh")) {
						new_msg = parts[2];
						action = true;
						
					}
					//If command is logout then
					if(request.getParameter("cmd") != null){
						if(request.getParameter("cmd").equals("logout")) {
							c[i].setMaxAge(0); //Set cookie expiration time to 0
							response.addCookie(c[i]); //Send new cookie
							
							//Remove
							RPCClientStub(3,session_id_c,sv1,ipp,parts[1]);
							
							RemoveCookie(session_id_c); //Remove entry from session table
														
							response.sendRedirect(request.getContextPath() + "/Project.jsp?cmd=logout"); //Display logout page
						}
					}
					
					if(!action) {
						new_msg = parts[2];
					}
					
					if(sv1 != null) {
						//Update entry in the session table	
						sv1.time_stamp = new Date().getTime() + ((minutes * 60 + 2*delta + tau) * 1000 ); 
						sv1.message = new_msg;
						int vno = Integer.parseInt(sv1.version_number);
						sv1.version_number = (++vno) + ""; //Increment version number
						
						sessionTable.put(session_id_c, sv1);
					}
					
					String backUpIP = RPCClientStub(2,session_id_c,sv1,ipp,"");
					
					String cookie_msg;
					if(backUpIP != ""){
						cookie_msg = parts[0]+"#"+ (Integer.parseInt(parts[1]) + 1) +"#"+new_msg + "#" + ippLocal + backUpIP;
					} else {
						cookie_msg = parts[0]+"#"+ (Integer.parseInt(parts[1]) + 1) +"#"+new_msg + "#" + ippLocal;
					}
					
					String ippStale = "";
					String[] backUpList = backUpIP.split("#");
					for(int j=0; j < ipp.size(); j++){
						if(! Arrays.asList(backUpList).contains(ipp.get(j))){
							ippStale = ipp.get(j);
							break;
						}
					}
					
					if(ippStale != ""){
						cookie_msg += "@" + ippStale;
					}
					
					c[i].setValue(cookie_msg);
					c[i].setMaxAge(minutes * 60 + delta);
					response.addCookie(c[i]);
				}
			}
		}
		if(request.getParameter("cmd") != null && request.getParameter("cmd").equals("logout")){
			
		} else if(!redirect){
			request.setAttribute("mbrSet", mbrSet);
			if(request.getParameter("first") != null && 
					request.getParameter("first").equals("true")){
				session_loc = 0;
			}
			request.setAttribute("location", session_loc+"");
	
			request.getRequestDispatcher("/Project.jsp").forward(request, response);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
