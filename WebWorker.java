/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

/*
 * CS 371-M01 Software Development
 * @co-author Sanford Johnston
 * @date February 9, 2018
 * Understanding of the webserver/webworker and how to satisfy the requirements
 * of Program 2 came from studying these resources
 * http://cs.au.dk/~amoeller/WWW/javaweb/server.html
 * https://en.wikipedia.org/wiki/Favicon
 * cs.fit.edu/~mmahoney/cse3103/java/Webserver.java
 */

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;  //class global variable

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   String fileName = "";
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream(); 	//INPUT STREAM COMES FROM BROWSER VIA WEBSERVER
      OutputStream os = socket.getOutputStream(); 	//OUTPUT STREAM GOES TO THE BROWSER DIRECTLY
      fileName = readHTTPRequest(is); 	
      
      // determine the MIME type and print HTTP header
      String mimeType = getContentType(fileName);	      
      
      writeHTTPHeader(os,mimeType);
      writeContent(os, fileName, mimeType);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{ 
   String line;
   String fileName = "";
     
   BufferedReader r = new BufferedReader(new InputStreamReader(is));   
   // request handler loop
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         // read first line of request
         line = r.readLine();
         System.err.println("Request line: ("+line+")");         
         if (fileName == "")
         {
        	 fileName = getFileName(line);
        	 break;
         }
      } 
      catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }      
   }
   
   return fileName;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   os.write("HTTP/1.1 200 OK\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Sanford's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write("rel=\"icon\" type=\"image/png\" href=\"favicon.png\"".getBytes());    
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
* @param st is a string used as an intermediary for passing a line of htm to os
* @param check is a temporary string for identifying one of two special tags which are to be substituted with by a date or the server name when passed to os
**/
private void writeContent(OutputStream os, String fileName, String mimeType) throws Exception
{
	String st = "";
	String check = "";
			
	// IF CONTENT IS HTML, SEND TEXT TO BROWSER
	// ELSE IF CONTENT IS IMAGE, SEND IMAGE TO BROWSER	
	try {
		// Load htm file sent from the run() class
		File file = new File(fileName);	
		// Use a BufferedReader to read text from a character input stream
		BufferedReader in = new BufferedReader(new FileReader(file));	
		if (mimeType == "text/html" || mimeType == "text/plain")
		{
			// display contents line by line while src = text
			while ((st = in.readLine()) != null)
			{		   
				// find special tags <cs371date> and <cs371server> and return alternate text
				check = st;
				// use the string check to look for tags <cs371date> and <cs371server>
				if (check.contains("cs371date")) 
				{
					st = "<span style='font-size:14.0pt;mso-bidi-font-size:11.0pt;line-height:\\n\" + \n" + 
							"	      		\"107%;color:#C00000'>" + getDate() + "<o:p><br /></o:p></span>" + "<br />";
					/* just for fun, loop through the String st and send it to the browser one character at a time
	    		char dateChar = ' ';
		    	for (int k = 0; k < st.length(); k++)
		    	{
				   dateChar = st.charAt(k);
				   os.write(dateChar);	    		
		    	}*/
					// passes content as binary
					os.write(st.getBytes());
				} // end if special tag 'date'
				else if (check.contains("cs371server")) 
				{
					st = "<span style='font-size:14.0pt;mso-bidi-font-size:11.0pt;line-height:\\n\" + \n" + 
							"	      		\"107%;color:#C00000'>Sanford J's Awesome Web Browser<o:p><br /></o:p></span>" + "<br />";
					// passes content as binary
					os.write(st.getBytes());
				} // end if special tag 'server'
				// Output the string of text to the web client
				else if (st.startsWith("src="))
				{
					check = getFileName(st);
					try
					{
						InputStream imageFile = new FileInputStream(check);
						/*st = "HTTP/1.1 200 OK\r\n" +			
							"Content-Type: " + mimeType + "\r\n" +
							"Date: " + getDate() + "\r\n" +
							"Server: FileServer 1.0\r\n\r\n";
						os.write(st.getBytes());*/
						sendImage(imageFile, os); // send raw image
					}
					catch (FileNotFoundException e)
					{
						// formatted htm of the 404 Not Found message
						st = "<html><body><p class=MsoNormal align=center style='text-align:center'><b style='mso-bidi-font-weight:\n" + 
								"normal'><span style='font-size:20.0pt;mso-bidi-font-size:11.0pt;line-height:\n" + 
								"107%;color:blue'>404 Not Found<o:p></o:p></span></b></p></body></html>\n";	      
						os.write(st.getBytes());
					}
				}
				else 
				{
					st = st + " ";
					os.write(st.getBytes());	    	
				} // end else not special tag
			} 
			in.close();
		} // end if		
	}
	catch (Exception e) {
		// formatted htm of the 404 Not Found message
		st = "<html><body><p class=MsoNormal align=center style='text-align:center'><b style='mso-bidi-font-weight:\n" + 
				"normal'><span style='font-size:20.0pt;mso-bidi-font-size:11.0pt;line-height:\n" + 
				"107%;color:#C00000'>404 Not Found<o:p></o:p></span></b></p></body></html>\n";	      
		os.write(st.getBytes());
		return;
	}
	// close html page tags
   os.write("<h3>My web server works!</h3>\n".getBytes());
   os.write("</body></html>\n".getBytes());
}

/**
 * Pulls the date from the system clock, converts it to a pleasing format and returns it as a string
 * @param date: a Date object
 * @param niceDate: a Date.Format object of date
 * @return: a string of the formatted date from the system clock
 */
private String getDate() {
	Date date = new Date();
	String niceDate = DateFormat.getDateInstance().format(date);
	return niceDate;
}

/**
 *  Relying on the filename extension, the mime type for the file 
 * is determine and returned to the calling method.
 * @param fileName is the name of the file
 * @return string of the correct image content type
 */
private static String getContentType(String fileName)
{
	String mimeType = "";
	if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
    {
  	  mimeType = "text/html";
    }
    else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
    {
  	  mimeType = "image/jpeg";
    }
    else if (fileName.endsWith(".gif"))
    {
  	  mimeType = "image/gif";
    }
    else if (fileName.endsWith(".png"))
    {
  	  mimeType = "image/png";
    }
    else
    {
  	  mimeType = "text/plain";
    } 
	return mimeType;
}

/**
 * Gets the name of the file from a string, line
 * @param line a string of the first GET request line from the browser
 * @return fileName, the name of the file
 */
private static String getFileName(String line)
{
	String fileName = "";
	int stringIndex = 0;
	char dot = '0';
	// if line is empty, return an empty string
	if (line.length()==0) 
	{
		System.out.println("File name: " + fileName);
		return fileName;
	} 
	// parse the file name out of the first line of request                        
	// loop to get past initial undesired characters
	while (dot != '/'&& dot != '\"')
	{
		dot = line.charAt(stringIndex);
		stringIndex++;
	}
	//Retrieves the name of the file so long as the file name does not include a '.' 
	while (stringIndex < line.length() && dot != '.')
	{            
		dot = line.charAt(stringIndex);   
		fileName = fileName + dot;
		stringIndex++;
	}
	// add the file extension by searching for space
	dot = line.charAt(stringIndex);
	while (stringIndex < line.length() && dot != ' ' && dot != '\"')
	{
		fileName = fileName + dot;
		stringIndex++;
		dot = line.charAt(stringIndex);		
	}	        
	// end parse the file name out of the first line of request

	return fileName;
}

/**
 * Sends an image to the browser
 * @param file the name of the file to be displayed
 * @param out the output stream to the browser
 */
private static void sendImage(InputStream file, OutputStream out)
{
	try 
	{
		byte [] buffer = new byte[10000];
		while (file.available()>0)
			out.write(buffer, 0, file.read(buffer));
	}
	catch (IOException e)
	{
		System.err.println(e);
	}
	
}

} // end class
