<!--
	Authors: Suman Somasundar, Herat Gandhi, Vinayaka Dattatraya
 	Last modified date: 26th February, 2013 
 -->
<%@page import="java.net.InetAddress"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="java.util.*,java.sql.Timestamp,java.net.*,java.io.*;" session="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Project 1</title>
</head>
<body>
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
		
		String[] parts = null;
		if(c != null) {
			for (int i=0;i<c.length;i++) {
				if(c[i].getName().equals("CS5300PROJ1SESSIONSVH")) {
					parts = c[i].getValue().split("#");
					msg = parts[2];
					maxage = c[i].getMaxAge();
				}
			}
			//Display all the relevant data and controls
			out.println("<h2>" + msg + "</h2>");
			out.println("<table>");
			out.println("<tr><form method='get' action='"+ request.getContextPath() +"/Project1'>");
			out.println("<td><input type='text' name='replace' size='30'/></td>");
			out.println("<td><input type='submit' value='Replace' /></td>");
			out.println("</form></tr>");
			out.println("<tr><td><a href='"+ request.getContextPath() +"/Project1?cmd=refresh'><button>Refresh</button></a></td></tr>");
			out.println("<tr><td><a href='"+ request.getContextPath() +"/Project1?cmd=logout'><button>Logout</button></a></td></tr>");
			out.println("<tr><td><a href='"+ request.getContextPath() +"/Project1?cmd=error&instance='"+ retrieveInstanceId() +"><button>Stop Server</button></a></td></tr>");
			out.println("</table>");

			out.println("Session expires on: "+ new Timestamp( new Date().getTime() + ((minutes * 60 + delta) * 1000)));
			out.println("<br/>Server IP:" + serverIP);
			out.println("<br/>Port:"+request.getLocalPort());
			out.println("<br/>Server ID: "+retrieveInstanceId());
			
			ArrayList<String> mbrSet = (ArrayList<String>)request.getAttribute("mbrSet");
			String res = "";
			if(mbrSet != null) {
				for(String line : mbrSet) {
					res += line+"<br/>";
				}
			}
			
			out.println("<br/>mbrSet: "+res);
			String session_locc="";
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
			out.println("<br/>Session found at: "+session_locc + "  " + (String)request.getAttribute("location"));
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
			response.sendRedirect(request.getContextPath() +"/Project1");
		}
	}
	%>
	<%!
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