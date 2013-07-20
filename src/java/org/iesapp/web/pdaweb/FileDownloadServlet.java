package org.iesapp.web.pdaweb;

/*
 * 26-3-2013
 * Servlet per baixar-se fitxers
 * Josep
 */

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import org.iesapp.util.StringUtils;
import org.iesapp.web.cloudws.FacesUtil;

@WebServlet("/cloud/*")
public class FileDownloadServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
    static final long serialVersionUID = 1L;
    private static final int BUFSIZE = 4096;
    private String filePath;
    
    @Override
    public void init() {
        // Determine physical path
          //Retreive initialization parameter
        String fileHostingDir = StringUtils.noNull((String) FacesUtil.getWebXmlParam("pdaweb.fileHostingDir")).trim();

        if(fileHostingDir.isEmpty()){
             filePath = getServletContext().getRealPath("") + File.separator + "cloud" + File.separator;
        }
        else
        {
            filePath = fileHostingDir + File.separator;
        }
        
      
    }
    
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
         String disposition = request.getParameter("disposition");
         if (disposition == null || (!disposition.equals("inline") && !disposition.equals("attachment")) )
                  {
            disposition = "inline";
        }
        
        //Retrieves the file name
        String tmp =  String.valueOf(request.getPathInfo().substring(1));
        String userDir = normalize(StringUtils.BeforeFirst(tmp, "/"));  //Stands for user id
        String fileName = StringUtils.AfterLast(tmp, "/");
        String afterUserDir = StringUtils.AfterFirst(tmp, "/").toLowerCase();
        //Add /public from url
        
        File file = new File(filePath+tmp);
        
        //If is a file (download it) - if directory show list in html
        
        //Only files allocated in Public folders can be downloaded using this servlet
        boolean permisos = afterUserDir.startsWith("public");
        
        if( file.exists() && permisos )
        {
      
        
            if(file.isFile())
            {

                int length = 0;
                ServletOutputStream outStream = response.getOutputStream();
                ServletContext context = getServletConfig().getServletContext();
                String mimetype = context.getMimeType(fileName);

                // sets response content type
                if (mimetype == null) {
                    mimetype = "application/octet-stream";
                }
                response.setContentType(mimetype);
                response.setContentLength((int) file.length());

                // sets HTTP header
                response.setHeader("Content-Disposition", disposition + "; filename=\"" + fileName + "\""); //attachment

                byte[] byteBuffer = new byte[BUFSIZE];
                DataInputStream in = new DataInputStream(new FileInputStream(file));

                // reads the file's bytes and writes them to the response stream
                while ((in != null) && ((length = in.read(byteBuffer)) != -1)) {
                    outStream.write(byteBuffer, 0, length);
                }

                in.close();
                outStream.close();
            }
            
            
        }
        else
        {
              response.setContentType("text/plain");
              // sets HTTP header
              response.setHeader("Content-Disposition", "inline"); //attachment
              response.setStatus(Status.FORBIDDEN.getStatusCode());

        }
    }


   @Override
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
      doGet(request,response);
  }

    private String normalize(String input) {
        String output = StringUtils.noNull(input).trim();
        int n = output.length();
        if(n<2)
        {
            output = "00"+output;
        }
        else if(n<3)
        {
            output = "0"+output;
        }
        return output;
    }
    
    
   
}