/**
 * Authors: Suman Somasundar, Herat Gandhi, Vinayaka Dattatraya
 * Last modified date: 26th February, 2013
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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.StopInstancesRequest;

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

class CleanUpProcess extends TimerTask {
    public void run() {
    	/**
         * RemoveExpiredCookie method
         * This method iterates through the session table when it finds
         * the stale session entry, it removes that session entry 
         * from the session table.
         */
        for (Iterator<Map.Entry<String, SessionValue>> itr = Project1.sessionTable.entrySet().iterator(); itr.hasNext(); ) {
        	synchronized (itr) {
        		Map.Entry<String, SessionValue> entry = itr.next();
    		    /*
    		     * Check whether current timestamp is greater than session's
    		     * expiration time, if yes then remove that entry from session
    		     * table.
    		     */
    		    if (entry.getValue().time_stamp + 1000 < new Date().getTime()) {
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
	public static final int wait_time_seconds = 2;
	private static final int delta = 5;
	private static final int tau = 5;
	
	//Hash Table sessionTable is used to store session data
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
    
    String RPCClientStub(int opcode, String sessionid, SessionValue sv, String ipp1, String ipp2, String version) {
		try {		    
			switch(opcode) {
				case 1:
					//Session Read
					byte[] outBuf_r;
					byte[] inBuf_r = new byte[512];
					byte[] inBuf_r1 = new byte[512];
					int call_id_r = (int)(Math.random() * 1000);
					String packetS_r = call_id_r + "#" + opcode + "#" + sessionid + "#" + version.trim() + "#" + port_udp;
					outBuf_r = packetS_r.getBytes();
					
					InetAddress ipA_r1 = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
					
					int portA_r1 = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
					
					try {
						DatagramSocket clientSocket = new DatagramSocket();
						DatagramPacket sendPacket = new DatagramPacket(outBuf_r, outBuf_r.length, ipA_r1, portA_r1);
					    clientSocket.send(sendPacket);
					    
					    clientSocket.setSoTimeout(wait_time_seconds * 1000);
					    
					    DatagramPacket receivePacket = new DatagramPacket(inBuf_r, inBuf_r.length);
					    clientSocket.receive(receivePacket);
					    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
					    int callID = Integer.parseInt(packetList[0]);
					    if(callID == call_id_r) {
					    	session_loc = 1;
					    	return new String(inBuf_r,0,receivePacket.getLength());
					    }
					} catch(Exception e) {
						mbrSet.remove(ipp1);
					}
					
					InetAddress ipA_r2 = null;
					int portA_r2 = 0;
					if(ipp2 != "") {
						ipA_r2 = InetAddress.getByName(ipp2.substring(0,ipp2.indexOf(':')));
						portA_r2 = Integer.parseInt(ipp2.substring(ipp2.indexOf(':')+1));
					
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_r, outBuf_r.length, ipA_r2, portA_r2);
						    clientSocket.send(sendPacket);
						    
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    
						    DatagramPacket receivePacket = new DatagramPacket(inBuf_r1, inBuf_r1.length);
						    clientSocket.receive(receivePacket);
						    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
						    int callID = Integer.parseInt(packetList[0]);
						    if(callID == call_id_r) {
						    	session_loc = 2;
						    	return new String(inBuf_r1);
						    }
						} catch(Exception e) {
							mbrSet.remove(ipp2);
							return null;
						}
					} else return null;
					
				case 2:
					//Session Update or write
					byte[] outBuf_u;
					byte[] inBuf_u = new byte[512];
					int call_id_u = (int)(Math.random() * 1000);
					String packetS4_u = call_id_u + "#" + opcode + "#" + sessionid + "#" + sv.message + "#" + sv.version_number + "#" + sv.time_stamp + "#" + port_udp;
					outBuf_u = packetS4_u.getBytes();
					String ack = "NAK"; 
					
					if(ipp1 != "") {
						InetAddress ipA4_u = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
						int portA4_u = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipA4_u, portA4_u);
						    clientSocket.send(sendPacket);
						    
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    
						    DatagramPacket receivePacket = new DatagramPacket(inBuf_u, inBuf_u.length);
						    clientSocket.receive(receivePacket);
						    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
						    int callID = Integer.parseInt(packetList[0]);
						    if(callID == call_id_u) {
						    	ack = packetList[1];
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
						InetAddress ipA42_u = InetAddress.getByName(ipp2.substring(0,ipp2.indexOf(':')));
						int portA42_u = Integer.parseInt(ipp2.substring(ipp2.indexOf(':')+1));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipA42_u, portA42_u);
						    clientSocket.send(sendPacket);
						    
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    
						    DatagramPacket receivePacket = new DatagramPacket(inBuf_u, inBuf_u.length);
						    clientSocket.receive(receivePacket);
						    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
						    int callID = Integer.parseInt(packetList[0]);
						    if(callID == call_id_u) {
						    	ack = packetList[1];
						    	if(ack.equals("ACK")){
						    		return ipp2;
						    	}
						    }
						} catch(Exception e) {
							mbrSet.remove(ipp2);
							ack = "NAK";
						}
					}
					if (ack.equals("NAK") && mbrSet.size() > 0) {
						int randomNode;
						InetAddress ipA4_u;
						int portA4_u;
						do{
							randomNode = (int)(Math.random() * mbrSet.size());
							String ipp = mbrSet.get(randomNode);
							ipA4_u = InetAddress.getByName(ipp.substring(0,ipp.indexOf(':')));
							portA4_u = Integer.parseInt(ipp.substring(ipp.indexOf(':')+1));
							try{
								DatagramSocket clientSocket = new DatagramSocket();
								DatagramPacket sendPacket = new DatagramPacket(outBuf_u, outBuf_u.length, ipA4_u, portA4_u);
							    clientSocket.send(sendPacket);
							    
							    clientSocket.setSoTimeout(wait_time_seconds * 1000);
							    
							    DatagramPacket receivePacket = new DatagramPacket(inBuf_u, inBuf_u.length);
							    clientSocket.receive(receivePacket);
							    String[] packetList = new String(receivePacket.getData(),0,receivePacket.getLength()).split("#");
							    int callID = Integer.parseInt(packetList[0]);
							    if(callID == call_id_u) {
							    	ack = packetList[1];
							    	if(ack.equals("ACK")){
							    		return ipp;
							    	}
							    }
							} catch (Exception e) {
								mbrSet.remove(ipp);
							}
						}while(mbrSet.size() > 0 && ack.equals("NAK"));
						
					}
				break;
				case 3:
					//Session Delete
					byte[] outBuf4;
					byte[] inBuf4 = new byte[512];
					int call_id4 = (int)(Math.random() * 1000);
					String packetS4 = call_id4 + "#" + opcode + "#" + sessionid + "#" + version + "#" + port_udp;
					outBuf4 = packetS4.getBytes();
					InetAddress ipA4;
					int portA4;
					if (!ipp1.equals("")) {
						ipA4 = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
						portA4 = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
						
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf4, outBuf4.length, ipA4, portA4);
						    clientSocket.send(sendPacket);
						    
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    
						    DatagramPacket receivePacket = new DatagramPacket(inBuf4, inBuf4.length);
						    clientSocket.receive(receivePacket);				    
						} catch(Exception e) {
							mbrSet.remove(ipp1);
						}
					}
					if(ipp2 != "") {
						InetAddress ipA42 = InetAddress.getByName(ipp2.substring(0,ipp2.indexOf(':')));
						int portA42 = Integer.parseInt(ipp2.substring(ipp2.indexOf(':')+1));
						try {
							DatagramSocket clientSocket = new DatagramSocket();
							DatagramPacket sendPacket = new DatagramPacket(outBuf4, outBuf4.length, ipA42, portA42);
						    clientSocket.send(sendPacket);
						    
						    clientSocket.setSoTimeout(wait_time_seconds * 1000);
						    
						    DatagramPacket receivePacket = new DatagramPacket(inBuf4, inBuf4.length);
						    clientSocket.receive(receivePacket);				    
						} catch(Exception e) {
							mbrSet.remove(ipp2);
						}
					}
				break;
				case 4:
					byte[] outBuf5;
					byte[] inBuf5 = new byte[512];
					int call_id5 = (int)(Math.random() * 1000);
					String packetS5 = call_id5 + "#" + opcode + "#" + version + "#" + port_udp;
					outBuf5 = packetS5.getBytes();
					ipA4 = InetAddress.getByName(ipp1.substring(0,ipp1.indexOf(':')));
					portA4 = Integer.parseInt(ipp1.substring(ipp1.indexOf(':')+1));
					
					try {
						DatagramSocket clientSocket = new DatagramSocket();
						DatagramPacket sendPacket = new DatagramPacket(outBuf5, outBuf5.length, ipA4, portA4);
					    clientSocket.send(sendPacket);
					    
					    clientSocket.setSoTimeout(wait_time_seconds * 1000);
					    
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
						//mbrSet.remove(ipp1);
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
		String ippLocal = InetAddress.getLocalHost().getHostAddress().replace("/", "") + ":" + port_udp;
		boolean redirect = false;
		
		System.out.println("Request Arrived!");
		
		if(request.getParameter("cmd") != null && request.getParameter("cmd") == "error") {
			BasicAWSCredentials awsCredentials = new BasicAWSCredentials("AKIAJ22NLENRGJQT4Z2Q", "Y9VFisUjD9NKGntXhjZsGvUQo8cuS4BXgg20FdWL");

			AmazonEC2Client ec2Client = new AmazonEC2Client(awsCredentials);
			ec2Client.setEndpoint("");/*ec2.us-west-1.amazonaws.com*/

			List<String> instancesToStop = new ArrayList<String>();
	        instancesToStop.add(request.getParameter("instance"));
	        StopInstancesRequest stoptr = new StopInstancesRequest();                
	        stoptr.setInstanceIds(instancesToStop);
	        ec2Client.stopInstances(stoptr);
			return;
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
						RPCClientStub(4, "", null, ipp1, "", "4");        //get 4 members from ipp1 
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
					
					if(parts.length > 5) {
						String ippStale = parts[5];
						if(!ippStale.equals(ippLocal)){
							SessionValue sv1 = null;
							RPCClientStub(3, session_id_c, sv1,"", ippStale, parts[1]);
						}
					}
					
					if(!ippLocal.equals(ipp1) && mbrSet.indexOf(ipp1) == -1) {
						mbrSet.add(ipp1.replace("/", ""));
					}
					
					//Get session value corresponding to the above session id
					//### If session data is not available here then go and fetch from other servers
					SessionValue sv1 = (SessionValue) sessionTable.get(session_id_c);
					
					if(sv1 == null || (sv1 != null && Integer.parseInt(sv1.version_number) < Integer.parseInt(parts[1]))) {
						String sv1_data = RPCClientStub(1, session_id_c, null, ipp1, ipp2, parts[1] );
						
						if (sv1_data == null) {
							c[i].setMaxAge(0);
							response.addCookie(c[i]);
							response.sendRedirect(request.getContextPath() + "/ErrorPage.jsp");
							redirect = true;
						} else {
							sv1 = new SessionValue();
							
							System.out.println("data:" + sv1_data);
							
							String[] parts1 = sv1_data.split("#");
							sv1.message = parts1[2];
							sv1.version_number = parts1[3];
							sv1.time_stamp = Long.parseLong(parts1[4]);
							//sessionTable.put(session_id_c, sv1);
						}
					} else if(sv1 != null && Integer.parseInt(sv1.version_number) >= Integer.parseInt(parts[1])) {
						session_loc = 3;
						System.out.println("From Table");
					}
					
					//Get message value from cookie
					//msg = parts[2];
								
					
					/*There is not another ipp in the cookie
					if(ipp_tpl.indexOf("@") != -1) {
						ipp1 = ipp_tpl.substring(0,ipp_tpl.indexOf("@"));
						ipp2 = ipp_tpl.substring(ipp_tpl.indexOf("@")+1);
					} */
					
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
			
			//response.sendRedirect(request.getContextPath() + "/Project.jsp"); //Redirect to the jsp file
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
