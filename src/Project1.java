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
	public static final int minutes = 5; //minutes after which delete cookie
	public static final int wait_time_seconds = 2; //Time for which we should wait for acknowledgement
	private static final int delta = 5; //Value of delta
	private static final int tau = 5; //Value of tau
	
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
					//Get InetAddress object for IPP Primary
					InetAddress ipA_r1 = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
					//Get port number from cookie
					int portA_r1 = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
					try {
						DatagramSocket clientSocket = new DatagramSocket();
						DatagramPacket sendPacket = new DatagramPacket(outBuf_r, outBuf_r.length, ipA_r1, portA_r1);
						//Send Session Read request to IPP Primary
					    clientSocket.send(sendPacket);
					    //Wait for IPP Primary's reply
					    clientSocket.setSoTimeout(wait_time_seconds * 1000);
					    //Get reply
					    DatagramPacket receivePacket = new DatagramPacket(inBuf_r, inBuf_r.length);
					    clientSocket.receive(receivePacket);
					    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
					    int callID = Integer.parseInt(packetList[0]);
					    //Comapare call ids of incoming and outgoing packets
					    if(callID == call_id_r) {
					    	session_loc = 1;
					    	//Show session found from IPP Primary
					    	return new String(inBuf_r,0,receivePacket.getLength());
					    }
					} catch(Exception e) {
						mbrSet.remove(ipp1);
					}
					//If IPP Primary failes to reply then try to read session from IPP Backup
					InetAddress ipA_r2 = null;
					int portA_r2 = 0;
					if(ipp2 != "") {
						//Get InetAddress and port
						ipA_r2 = InetAddress.getByName(ipp2.substring(0,ipp2.indexOf(':')));
						portA_r2 = Integer.parseInt(ipp2.substring(ipp2.indexOf(':')+1));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_r, outBuf_r.length, ipA_r2, portA_r2);
						    //Send session read request
							clientSocket.send(sendPacket);
						    //Wait for some time for response
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    //Receive session data
						    DatagramPacket receivePacket = new DatagramPacket(inBuf_r1, inBuf_r1.length);
						    clientSocket.receive(receivePacket);
						    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
						    int callID = Integer.parseInt(packetList[0]);
						    //Compare call id
						    if(callID == call_id_r) {
						    	session_loc = 2;
						    	//Display that session was found from IPP Backup
						    	return new String(inBuf_r1);
						    }
						} catch(Exception e) {
							mbrSet.remove(ipp2);
							return null;
						}
					} else return null; //If session not found from IPP Primary or Backup then raise error					
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
					String ack = "NAK"; 
					
					if(ipp1 != "") {
						//First try to write/update session at IPP Primary if that exists 
						InetAddress ipA4_u = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
						int portA4_u = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipA4_u, portA4_u);
							//Send session related data
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
						    	ack = packetList[1];
						    	//Check whether write operation succeeded or not
						    	if(ack.equals("ACK")){
						    		return ipp1;
						    	}
						    }
						} catch(Exception e) {
							mbrSet.remove(ipp1);
							ack = "NAK";
						}
					} 
					
					if(ack.equals("NAK") && ipp2 != "") {
						//Write at IPP Backup if it exists and if write request failed at the primary server
						InetAddress ipA42_u = InetAddress.getByName(ipp2.substring(0,ipp2.indexOf(':')));
						int portA42_u = Integer.parseInt(ipp2.substring(ipp2.indexOf(':')+1));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipA42_u, portA42_u);
							//Send update/write data
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
						    		return ipp2;
						    	}
						    }
						} catch(Exception e) {
							mbrSet.remove(ipp2);
							ack = "NAK";
						}
					}
					/**
					 * If IPP Primary does not exist or write request failed at IPP Primary
					 * and/or If IPP Backup does not exist or write request failed at IPP Backup
					 * and If mbrSet is not null then try to write session at one of the servers
					 */					
					if (ack.equals("NAK") && mbrSet.size() > 0) {
						int randomNode;
						InetAddress ipA4_u;
						int portA4_u;
						//Repeat until ack has been received or mbrSet is null
						do {
							//Set IP and Port addresses
							randomNode = (int)(Math.random() * mbrSet.size());
							String ipp = mbrSet.get(randomNode);
							ipA4_u = InetAddress.getByName(ipp.substring(0,ipp.indexOf(':')));
							portA4_u = Integer.parseInt(ipp.substring(ipp.indexOf(':')+1));
							try {
								DatagramSocket clientSocket = new DatagramSocket();
								DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipA4_u, portA4_u);
								//Send packet
							    clientSocket.send(sendPacket);
							    //Wait for some time
							    clientSocket.setSoTimeout(wait_time_seconds * 1000);
							    //Receive data
							    DatagramPacket receivePacket = new DatagramPacket(inBuf_u, inBuf_u.length);
							    clientSocket.receive(receivePacket);
							    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
							    int callID = Integer.parseInt(packetList[0]);
							    //Compare call ids
							    if(callID == call_id_u) {
							    	ack = packetList[1];
							    	//Check for the acknowledgement
							    	if(ack.equals("ACK")){
							    		return ipp;
							    	}
							    }
							} catch (Exception e) {
								mbrSet.remove(ipp);
							}
						} while(mbrSet.size() > 0 && ack.equals("NAK"));
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
					if (!ipp1.equals("")) {
						ipA4 = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
						portA4 = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
						//Remove session table entry from IPP Primary
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
							mbrSet.remove(ipp1);
						}
					}
					if(ipp2 != "") {
						//Remove session table entry from the IPP Backup
						InetAddress ipA42 = InetAddress.getByName(ipp2.substring(0,ipp2.indexOf(':')));
						int portA42 = Integer.parseInt(ipp2.substring(ipp2.indexOf(':')+1));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf4, outBuf4.length, ipA42, portA42);
							//Send delete request
						    clientSocket.send(sendPacket);
						    //Wait for the ack
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    //Get data
						    DatagramPacket receivePacket = new DatagramPacket(inBuf4, inBuf4.length);
						    clientSocket.receive(receivePacket);				    
						} catch(Exception e) {
							mbrSet.remove(ipp2);
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
					//Get IP Address and port address
					ipA4 = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
					portA4 = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));

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
					    //Get mbrSet and add the mbrSet to current server's mbrSet
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
				backup_n = RPCClientStub(2,session_id,sv,"","","");
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
					String[] parts = c[i].getValue().split("#"); 
					session_id_c = parts[0];
					
					String ipp1 = parts[3];
					
					if(mbrSet.size() == 0 && !ippLocal.equals(ipp1)) { 
						RPCClientStub(4, "", null, ipp1, "", "4");   //get 4 members from ipp1 
					} else if(mbrSet.size() == 0 && parts.length > 4 ) {
						RPCClientStub(4, "", null, parts[4], "", "4");
					}
					
					String ipp2 = "";
					if(parts.length > 4) {
						ipp2 = parts[4];
						if(!ippLocal.equals(ipp2) && mbrSet.indexOf(ipp2) == -1) {
							mbrSet.add(ipp2.replace("/", ""));
						}
					}
					//If cookie has stale IPP then send the RPC delete request to that IPP
					if(parts.length > 5) {
						String ippStale = parts[5];
						if(!ippStale.equals(ippLocal)){
							SessionValue sv1 = null;
							RPCClientStub(3, session_id_c, sv1,"", ippStale, parts[1]);
						}
					}
					//Add IPP to the mbrSet
					if(!ippLocal.equals(ipp1) && mbrSet.indexOf(ipp1) == -1) {
						mbrSet.add(ipp1.replace("/", ""));
					}
					
					//Get session value corresponding to the above session id
					//If session data is not available here then go and fetch from other servers
					SessionValue sv1 = (SessionValue) sessionTable.get(session_id_c);
					
					if(sv1 == null || (sv1 != null && Integer.parseInt(sv1.version_number) < Integer.parseInt(parts[1]))) {
						String sv1_data = RPCClientStub(1, session_id_c, null, ipp1, ipp2, parts[1] );
						
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
							
							//Remove from other servers
							if(ippLocal.equals(ipp1)) {
								RPCClientStub(3,session_id_c,sv1,"",ipp2,parts[1]);
							}
							else if(ippLocal.equals(ipp2)) {
								RPCClientStub(3,session_id_c,sv1,ipp1,"",parts[1]);
							}
							else RPCClientStub(3,session_id_c,sv1,ipp1,ipp2,parts[1]);
							
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
					
					String backUpIP = "";
					if (mbrSet.size() > 0) {
						if(ippLocal.equals(ipp1)) {
							backUpIP = RPCClientStub(2,session_id_c,sv1,"",ipp2,"");
						}
						else if(ippLocal.equals(ipp2)) {
							backUpIP = RPCClientStub(2,session_id_c,sv1,ipp1,"","");
						}
						else backUpIP = RPCClientStub(2,session_id_c,sv1,ipp1,ipp2,"");
					}
					
					String cookie_msg;
					if(backUpIP != "" && !backUpIP.equals(ipp1) && !backUpIP.equals(ippLocal)) {
						cookie_msg = parts[0]+"#"+ (Integer.parseInt(parts[1]) + 1) +"#"+new_msg + "#" + ippLocal + "#" + backUpIP + "#" + ipp1;
					} else if(backUpIP != "" && !backUpIP.equals(ipp2) && !backUpIP.equals(ippLocal)) {
						cookie_msg = parts[0]+"#"+ (Integer.parseInt(parts[1]) + 1) +"#"+new_msg + "#" + ippLocal + "#" + backUpIP + "#" + ipp2;
					} else {
						cookie_msg = parts[0]+"#"+ (Integer.parseInt(parts[1]) + 1) +"#"+new_msg + "#" + ippLocal;
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
