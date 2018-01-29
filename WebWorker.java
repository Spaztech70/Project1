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

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
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
      InputStream  is = socket.getInputStream(); 	//FROM WHERE IS THIS INPUT STREAM COMING?
      OutputStream os = socket.getOutputStream(); 	//TO WHERE IS THIS OUTPUT STREAM GOING?
      fileName = readHTTPRequest(is); 					//
      writeHTTPHeader(os,"text/html");
      writeContent(os, fileName);
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
   int stringIndex = 0;
   char dot = '0';  
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
                                  
         if (fileName == "")
         {
            while (dot != '/')
               {
                  dot = line.charAt(stringIndex);
                  stringIndex++;
               }
            while (stringIndex < line.length() && dot != '.')
            {            
               dot = line.charAt(stringIndex);   
               fileName = fileName + dot;
               stringIndex++;
            }
            fileName = fileName + "htm";
         }   
         if (line.length()==0) 
        	 {
        	 System.out.println("File name: " + fileName);
        	 break;
        	 }
         
      } catch (Exception e) {
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
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String fileName) throws Exception
{
	String st = "";
	String check = "";
			
	try {
		File file = new File(fileName);	
		//InputStream inputfile = new FileInputStream(fileName);
		BufferedReader in = new BufferedReader(new FileReader(file));
		
	    while ((st = in.readLine()) != null)
		{		   
	    	check = st;
	    	// use the string check to look for tags <cs371date> and <cs371server>
	    	if (check.contains("cs371date")) 
	    	{
	    		st = "<span style='font-size:14.0pt;mso-bidi-font-size:11.0pt;line-height:\\n\" + \n" + 
						"	      		\"107%;color:#C00000'>" + getDate() + "<o:p><br /></o:p></span>" + "<br />";
	    		char dateChar = ' ';
		    	for (int k = 0; k < st.length(); k++)
		    	{
				   dateChar = st.charAt(k);
				   os.write(dateChar);	    		
		    	}
	    		//os.write(st.getBytes());
	    	}
	    	else if (check.contains("cs371server")) 
				{
				st = "<span style='font-size:14.0pt;mso-bidi-font-size:11.0pt;line-height:\\n\" + \n" + 
					"	      		\"107%;color:#C00000'>Sanford J's Awesome Web Browser<o:p><br /></o:p></span>" + "<br />";
				os.write(st.getBytes());
				}
			else 
			{
	    	/*char oneChar = ' ';
	    	for (int i = 0; i < st.length(); i++)
	    	{
			   oneChar = st.charAt(i);
			   os.write(oneChar);	    		
	    	}*/
	    	os.write(st.getBytes());
	    	//System.out.println(st);
			}
		} 
	}
	catch (Exception e) {
	      st = "<html><body><p class=MsoNormal align=center style='text-align:center'><b style='mso-bidi-font-weight:\n" + 
	      		"normal'><span style='font-size:20.0pt;mso-bidi-font-size:11.0pt;line-height:\n" + 
	      		"107%;color:#C00000'>404 Not Found<o:p></o:p></span></b></p></body></html>\n";
	      /*char errChar = ' ';
	      for (int j = 0; j < st.length(); j++)
	      {
	    	  errChar = st.charAt(j);
	    	  os.write(errChar);
	      }*/
	      os.write(st.getBytes());
	      return;
	}
	  
   os.write("<h3>My web server works!</h3>\n".getBytes());
   os.write("</body></html>\n".getBytes());
}

private String getDate() {
	Date date = new Date();
	String niceDate = DateFormat.getDateInstance().format(date);
	return niceDate;
}

} // end class
