/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.cloudws;

import java.io.File;
import java.io.FileFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.iesapp.database.MyDatabase;

/**
 *
 * @author Josep
 */
public class Wrapper {
    
    //Default parameter values
    protected MyDatabase sgd;
    protected HashMap<String,Session> sessions;
    protected HashMap<String, String> dbInitParam;
    protected int anyAcademic;
    protected String fileHostingDir;
    protected String fileHostingLimit;
    protected FileFilter fileFilter;
    protected String clamscanAddr = "127.0.0.1";
    protected int clamscanPort = 3310;
    protected int clamscanTimeout = 10;
    protected boolean fileHostingUseClamscan = false;
    protected String fileHostingDownloadPath="";
    protected long fileHostingLimitBytes;
    private final ServletContext context;

  
    public Wrapper(ServletContext context) {
        this.context = context;
        
          //Initializes mysql to retrieve dbInit Param required for sgd initialization
         //mysql connections are required in order to validate user access
         //String host, String db, String user, String pass
            MyDatabase mysql = new MyDatabase(getInitParameter("pdaweb.mysqlHost"),
                    getInitParameter("pdaweb.mysqlDBPrefix"),
                    getInitParameter("pdaweb.mysqlUser"),
                    getInitParameter("pdaweb.mysqlPwd"),
                    getInitParameter("pdaweb.mysqlParams").replaceAll("$", "&"));
            mysql.connect();

            dbInitParam = new HashMap<String, String>();
            String SQL1 = "SELECT property, value FROM configuracio";
            try {
                Statement st = mysql.createStatement();
                ResultSet rs1 = mysql.getResultSet(SQL1);
                while (rs1 != null && rs1.next()) {
                    dbInitParam.put(rs1.getString(1), rs1.getString(2));
                }
                rs1.close();
                st.close();
            } catch (SQLException ex) {
                Logger.getLogger(GenericResource.class.getName()).log(Level.SEVERE, null, ex);
            }
            mysql.close();

            anyAcademic = Integer.parseInt(dbInitParam.get("anyIniciCurs"));
      
            
            //UPLOAD/DOWNLOAD - Limits de pujada de fitxers
            fileHostingDownloadPath = getInitParameter("pdaweb.fileHostingDownloadPath");
            fileHostingDir = getInitParameter("pdaweb.fileHostingDir");
            if(!fileHostingDir.endsWith(File.separator))
            {
               fileHostingDir += File.separator; 
            }
        
            fileHostingLimit = getInitParameter("pdaweb.fileHostingLimit").trim();
            fileHostingLimitBytes = 0;
            
            if(!fileHostingLimit.equalsIgnoreCase("unlimited"))
            {
                System.out.println("fileHostingLimit = "+fileHostingLimit);
                int len = fileHostingLimit.length();
                String value = fileHostingLimit.substring(0,len-1);
                String magnitud = fileHostingLimit.substring(len-1,len);
                int ival = Integer.parseInt(value);
                long factor = 1;
                if(magnitud.equalsIgnoreCase("k"))
                {
                    factor  = FileInfo.KB;
                }
                else if(magnitud.equalsIgnoreCase("m"))
                {
                    factor  = FileInfo.MB;
                }
                else if(magnitud.equalsIgnoreCase("g"))
                {
                    factor  = FileInfo.GB;
                }
                System.out.println("fileHostingLimit ival & factor "+ival+"&"+factor);

                fileHostingLimitBytes = ival*factor;
            }
            
            
            //Antivirus parameters
            clamscanAddr = getInitParameter("pdaweb.clamscanAddr");
            clamscanPort = Integer.parseInt(getInitParameter("pdaweb.clamscanPort"));
            clamscanTimeout = Integer.parseInt(getInitParameter("pdaweb.clamscanTimeout"));
            fileHostingUseClamscan = getInitParameter("pdaweb.fileHostingUseClamscan").toUpperCase().startsWith("Y");
            

    }
    
    //Lazy initialization

    public synchronized MyDatabase getSgd() {
        if(sgd==null || sgd.isClosed())
        {
            sgd = new MyDatabase(dbInitParam.get("sgdHost"),
                    dbInitParam.get("sgdDBPrefix") + anyAcademic,
                    dbInitParam.get("sgdUser"),
                    dbInitParam.get("sgdPasswd"),
                    dbInitParam.get("sgdDBParams"));
            sgd.connect();
        }
        return sgd;
    }

    public synchronized HashMap<String,Session> getSessions() {
        if(sessions==null)
        {
            sessions = new HashMap<String,Session>();
        }
        return sessions;
    }

    public synchronized HashMap<String, String> getDbInitParam() {
        return dbInitParam;
    }

    public synchronized int getAnyAcademic() {
        return anyAcademic;
    }

    public synchronized String getFileHostingDir() {
        return fileHostingDir;
    }

    public synchronized String getFileHostingLimit() {
        return fileHostingLimit;
    }

    public synchronized FileFilter getFileFilter() {
        if(fileFilter==null)
        {
           fileFilter = new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return !pathname.getName().startsWith(".");
                }
            };
        
        }
        return fileFilter;
    }

    public synchronized String getClamscanAddr() {
        return clamscanAddr;
    }

    public synchronized int getClamscanPort() {
        return clamscanPort;
    }

    public synchronized int getClamscanTimeout() {
        return clamscanTimeout;
    }

    public synchronized boolean isFileHostingUseClamscan() {
        return fileHostingUseClamscan;
    }

    public synchronized String getFileHostingDownloadPath() {
        return fileHostingDownloadPath;
    }

    public synchronized long getFileHostingLimitBytes() {
        return fileHostingLimitBytes;
    }


    private String getInitParameter(String key) {
        String str = (String) context.getInitParameter(key);
        return str==null?"":str;
    }
}
