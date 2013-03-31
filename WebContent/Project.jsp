<!--
	Authors: Suman Somasundar, Herat Gandhi, Vinayaka Dattatraya
	This JSP file contains all the UI related data of this application. 
 -->
<%@page import="java.net.InetAddress"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="java.util.*,java.sql.Timestamp,java.net.*,java.io.*;" session="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Project 1</title>
<script>
	/**
	  * Display various time related data for the request.
	  */
	function dateFill() {
		var d=new Date();
		var d1 = new Date(d.getFullYear(),d.getMonth(),d.getDay(),d.getHours(),d.getMinutes()+5,d.getSeconds()+5);
		var d2 = new Date(d.getFullYear(),d.getMonth(),d.getDay(),d.getHours(),d.getMinutes()+5,d.getSeconds()+10);
		document.getElementById("date1").innerHTML = d1;
		document.getElementById("date2").innerHTML = d2;
	}
</script>
</head>
<body onload="dateFill()">
	<%
	//If the command is logout then display logout message
	if(request.getParameter("cmd") != null && request.getParameter("cmd").equals("logout"))
	{
	%>
		<h2> Bye! </h2>
		<a href="<%= request.getContextPath() %>/Project1"><button>Home</button></a>
	<%
	}
	else
	{
		//Get cookie details for display purposes
		Cookie c[] = request.getCookies();
		String msg = "";
		int maxage = 0;
		InetAddress inetAdd= InetAddress.getLocalHost(); //Get server IP
		String serverIP = inetAdd.getHostAddress(); //Get server port
		
		int minutes = 5;
		int delta = 5;
		int tau = 5;
		
		String[] parts = null;
		if(c != null) {
			//Get cookie's data
			for (int i=0;i<c.length;i++) {
				if(c[i].getName().equals("CS5300PROJ1SESSIONSVH")) {
					parts = c[i].getValue().split("#");
					msg = parts[2];
					maxage = c[i].getMaxAge();
				}
			}
			
			out.println("<h2>" + msg + "</h2>"); //Display message
			out.println("<table>");
			out.println("<tr><form method='get' action='"+ request.getContextPath() +"/Project1'>");
			out.println("<td><input type='text' name='replace' size='30'/></td>"); //Display replace message textbox
			out.println("<td><input type='submit' value='Replace' /></td>"); //Display replace button
			out.println("</form></tr>");
			//Display refresh button
			out.println("<tr><td><a href='"+ request.getContextPath() +"/Project1?cmd=refresh'><button>Refresh</button></a></td></tr>");
			//Display Logout button
			out.println("<tr><td><a href='"+ request.getContextPath() +"/Project1?cmd=logout'><button>Logout</button></a></td></tr>");
			//Display stop instance button which will stop the current EC2 instance
			out.println("<tr><td><a href='"+ request.getContextPath() +"/Project1?cmd=error&instance="+ retrieveInstanceId() +"'><button>Stop Server</button></a></td></tr>");
			out.println("</table>");
			//Display cookie expiration time and the session discard time
			out.println("Cookie expires on: <span id='date1'></span>");
			out.println("<br/>Discard time: <span id='date2'></span>");
			//Display server ip
			out.println("<br/>Server IP:" + serverIP);
			//Display port
			out.println("<br/>Port:"+request.getLocalPort());
			//Get the EC2's instance id
			out.println("<br/>Server ID: "+retrieveInstanceId());
			//Display mbrSet
			ArrayList<String> mbrSet = (ArrayList<String>)request.getAttribute("mbrSet");
			String res = "";
			if(mbrSet != null) {
				for(String line : mbrSet) {
					res += line+"<br/>";
				}
			}
			
			out.println("<br/>mbrSet: "+res);
			String session_locc="";
			//Display where this session was found
			switch(Integer.parseInt((String)request.getAttribute("location"))) {
				case 0:
					session_locc = "New Session";
				break;
				case 1:
					session_locc = "IPP Primary";
				break;	
				case 2:
					session_locc = "IPP Backup";
				break;
				case 3:
					session_locc = "Session Table";
				break;
			}
			out.println("<br/>Session found at: "+session_locc);
			//Display IPP Primary and IPP Backup from the cookie
			if(c != null) {
				for (int i=0;i<c.length;i++) {
					if(c[i].getName().equals("CS5300PROJ1SESSIONSVH")) {
						out.println("<br/>IPP primary: " + parts[3]);
						if(parts.length > 4) {
							out.println("<br/>IPP backup: " + parts[4]);
						}
					}
				}
			}
		} else {
			//If the request does not contain the cookie then transfer the request to the servlet for cookie generation
			response.sendRedirect("Project1?first=true");
		}
	}
	%>
	<%!
	    /**
	     * RetrieveInstanceId function retrieves EC2 instance id and returns it as a String object
	     */
		public static String retrieveInstanceId() {
			String EC2Id = "";	
			try {
				String inputLine;
				URL EC2MetaData = new URL("http://169.254.169.254/latest/meta-data/instance-id");
				URLConnection EC2MD = EC2MetaData.openConnection();
				BufferedReader in = new BufferedReader(
				new InputStreamReader(
				EC2MD.getInputStream()));
				while ((inputLine = in.readLine()) != null) {	
					EC2Id = inputLine;
				}
				in.close();
				
			} catch(Exception e) {
				
			}
			return EC2Id;
		}
	%>
</body>
</html>