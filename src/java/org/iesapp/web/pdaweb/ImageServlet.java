/*
 * 2-8-2012
 * Aquest servlet proveeix imatges dels alumnes que
 * es troben com a blob en la base de dades,
 * Permet solucionar el problema de imatges dinamiques
 * <p:graphicimage del primefaces.
 * Josep
 */
package org.iesapp.web.pdaweb;


import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;


@WebServlet("/imagesdb/*")
public class ImageServlet extends HttpServlet {
    private Connection con;
    private String mysqlDBPrefix;

   
    @Override
    public void init() throws ServletException {
       establishConection();
   }
   
       
        
        
   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
       String relativePath = request.getParameter("file");
       if(relativePath==null)
       {
           doGetImageDB(request, response);
       }
       else
       {
           doGetFileImageIcon(request, response);
       }
  }


 @Override
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
      doGet(request,response);
  }
   
  private InputStream findImage(String imageId) {
         
         String SQL2 = "Select Foto from `"+mysqlDBPrefix+"`.xes_alumne_detall where Exp_FK_ID='"+imageId+"'";
         InputStream data = null;
         try {
            Statement st = con.createStatement();
            ResultSet rs2 = st.executeQuery(SQL2);
            
            while( rs2!=null && rs2.next() )
            {                
                 data = rs2.getBinaryStream("Foto");
            }
            if(rs2!=null) {
                 rs2.close();
             }
            if(st!=null) {
                 st.close();
             }
         
        } catch (SQLException ex) {
            Logger.getLogger(ImageServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         return data;
    }

    private void establishConection() {
       // Get the value of an initialization parameter
       String mysqlHost = getServletContext().getInitParameter("pdaweb.mysqlHost");
       mysqlDBPrefix = getServletContext().getInitParameter("pdaweb.mysqlDBPrefix");
       String mysqlUser = getServletContext().getInitParameter("pdaweb.mysqlUser");
       String mysqlPwd = getServletContext().getInitParameter("pdaweb.mysqlPwd");

       String url = "jdbc:mysql://" + mysqlHost + "/" + mysqlDBPrefix + "?zeroDateTimeBehavior=convertToNull";

       try {
           Class.forName("com.mysql.jdbc.Driver").newInstance();
           this.con = DriverManager.getConnection(url, mysqlUser, mysqlPwd);
       } catch (InstantiationException ex) {
           Logger.getLogger(ImageServlet.class.getName()).log(Level.SEVERE, null, ex);
       } catch (IllegalAccessException ex) {
           Logger.getLogger(ImageServlet.class.getName()).log(Level.SEVERE, null, ex);
       } catch (SQLException ex) {
           Logger.getLogger(ImageServlet.class.getName()).log(Level.SEVERE, null, ex);
       } catch (ClassNotFoundException ex) {
           Logger.getLogger(ImageServlet.class.getName()).log(Level.SEVERE, null, ex);
       }
    }
    
    /**
     * Returns an image from database
     * @param request
     * @param response 
     */

    private void doGetImageDB(HttpServletRequest request, HttpServletResponse response) {
       try {
            //Check conection
            if(con==null || con.isClosed()) {
                establishConection();
            }
        } catch (SQLException ex) {
            Logger.getLogger(ImageServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Retrieves the image id (expd)
        String pathInfo = request.getPathInfo();
        InputStream image = null;
        String imageId = "image";
        if(pathInfo!=null)
        {
           imageId = pathInfo.substring(1); // Gets string that goes after "/images/".
            //Return the image
           image = findImage(imageId); // Get Image from DB.
        }
        if(image==null) 
        {
             InputStream defaultPhoto = getServletContext().getResourceAsStream("/images/student.jpg");
             image = defaultPhoto;
        }
        String imageName = imageId+".jpg";
        response.setHeader("Content-Type", getServletContext().getMimeType(imageName));
        response.setHeader("Content-Disposition", "inline; filename=\"" + imageName + "\"");  //inline or attachment

        BufferedInputStream input = null;
        BufferedOutputStream output = null;

        try {

            input = new BufferedInputStream(image); // Creates buffered input stream.
            output = new BufferedOutputStream(response.getOutputStream());
            byte[] buffer = new byte[8192];
            for (int length = 0; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
        } catch (java.lang.Exception e) {
        }
        if (output != null) {
            try {
                output.close();
            } catch (IOException logOrIgnore) {
            }
        }
        if (input != null) {
            try {
                input.close();
            } catch (IOException logOrIgnore) {
            }
        }
    }

    /**
     * This servlet returs the image icon for a given file
     * @param request
     * @param response 
     */
    private void doGetFileImageIcon(HttpServletRequest request, HttpServletResponse response) {
        //Relative path
        String home = request.getSession().getServletContext().getInitParameter("pdaweb.fileHostingDir");
        if(!home.endsWith(File.separator))
        {
            home += File.separator;
        }
        String fileName = home + request.getParameter("file");
        File file = new File(fileName);
        FileSystemView fsv = FileSystemView.getFileSystemView();
        ImageIcon imageicon = (ImageIcon) fsv.getSystemIcon(file);
        InputStream imageStream = null;
        if (imageicon == null) {
            imageStream = getServletContext().getResourceAsStream("/images/ext/file.png");
        } else {
            //Convert imageicon to inputstream;
            Image im = imageicon.getImage();
            int width = im.getWidth(null);
            int heigth = im.getHeight(null);
            BufferedImage imatge = new BufferedImage(width, heigth, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = imatge.createGraphics();
            bg.drawImage(im, 0, 0, null);
            bg.dispose();

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(imatge, "png", baos);
                baos.flush();
                baos.close();
                byte[] toByteArray = baos.toByteArray();
                imageStream = new ByteArrayInputStream(toByteArray);
            } catch (Exception e) {
                //   
            }
        }

        String imageName = file.getName()+".jpg";
        response.setHeader("Content-Type", getServletContext().getMimeType(imageName));
        response.setHeader("Content-Disposition", "inline; filename=\"" + imageName + "\"");  //inline or attachment

        
        try {
             
            BufferedInputStream input = new BufferedInputStream(imageStream); // Creates buffered input stream.
            BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream());
            byte[] buffer = new byte[8192];
            for (int length = 0; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
            if (output != null) {
                 output.close();
            }
            if (input != null) {
                 input.close();
            }
        
        } 
        catch(java.lang.Exception e)  {
           
        }
  
    }
}