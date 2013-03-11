<!--
	Authors: Suman Somasundar, Herat Gandhi, Vinayaka Dattatraya
 	Last modified date: 26th February, 2013 
 -->
<%@page import="java.net.InetAddress"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="java.util.*,java.sql.Timestamp" session="false" %>
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
		
		if(c != null) {
			for (int i=0;i<c.length;i++) {
				if(c[i].getName().equals("CS5300PROJ1SESSIONSVH")) {
					String[] parts = c[i].getValue().split("#");
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
			out.println("<tr><td><a href='"+ request.getContextPath() +"/Project1?cmd=error'><button>Stop Server</button></a></td></tr>");
			out.println("</table>");

			out.println("Session expires on: "+ new Timestamp( new Date().getTime() + (5 * 60 * 1000) ));
			out.println("<br/>Server IP:" + serverIP);
			out.println("<br/>Port:"+request.getLocalPort());
		} else {
			response.sendRedirect(request.getContextPath() +"/Project1");
		}
	}
	%>
</body>
</html>