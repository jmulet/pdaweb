package org.iesapp.core.util;
 
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.iesapp.clients.iesdigital.ICoreData;
import org.iesapp.clients.sgd7.base.SgdBase;
import org.iesapp.database.MyDatabase;
import org.iesapp.util.StringUtils;
import org.iesapp.web.cloudws.FacesUtil;


/**
 *
 * @author Josep
 */
public final class CoreCfg extends ICoreData {

    //Constants - static members
    public static final String VERSION = "3.5.5";
    public static final String BUILD = "20130509";
    public static final int DBTYPE = 2; //1=access(NO LONGER SUPPORTED); 2=mysql
    public static final String prefix = "sig_";
    public static final String dl = "'";
     
    public boolean admin;

    //Stuff from .iesapp.ini
    public String core_pwdSU="";
    public String core_mysqlUser="";
    public String core_mysqlPasswd="";
    public String core_mysqlHost="";
    public String core_mysqlDB="";
    
    //Stuff from core.ini
    public byte core_showUsersWidget=1;
    public byte core_enableUsersNotes=1;
    public int  core_refreshUsersSeconds=60;
    public ArrayList<String> core_disableUsersNotesFor;
    
    //Stuff from DATABASE CONFIG TABLE
    public boolean coreDB_copiaLocal;
    public boolean coreDB_copiaExterna;
    public String  coreDB_sgdPasswd="";
    public String  coreDB_sgdUser="";
    public int     coreDB_autorefresc=0;
    public String  coreDB_doc="";
    public int     coreDB_guardiaAntelacio=0;
    public int     coreDB_tardaAntelacio=0;
    public int     coreDB_EditFitxes=1;
    public String  coreDB_FitxesDataPath="";
    public int     coreDB_timeoutSU=0;
    public String  coreDB_sgdHost="";
    public String  coreDB_sgdDB="";
    public int     coreDB_reservesAntelacio=15;
    public String  tipPasswd="";
    public ArrayList<String> lastLogins;

  //elements de la base de dades
    
     //LAZY-INITIALIZATION
     protected java.sql.Time horesClase[];
     protected java.sql.Time horesClase_fi[];
     protected ArrayList<java.sql.Date> festiusFrom;
     protected ArrayList<java.sql.Date> festiusTo;
     protected HashMap<String, HashMap> festiusJSON;
     
     //EAGER-INITIALIZATION
     public String core_pwdPREF="";
     public String core_pwdGUARD="";
     public String osName;
     
     //Two common databases
     protected MyDatabase mysql;
     protected MyDatabase sgd;
     private String startupPath = null;
  
    private static int id = 0;
    private static ArrayList<String> actions;
    public  String remoteAddr;
    public  String remoteHost;
    public  String core_mysqlParams;
    public String coreDB_sgdParams;
     
    //Stuff from database
    //DefaultConstructor
    public CoreCfg()
    {
        actions = new ArrayList<String>();
        
        //Read information from WEBXML PARAMS
        core_mysqlHost= (String) FacesUtil.getWebXmlParam("pdaweb.mysqlHost");
        core_mysqlDBPrefix= (String) FacesUtil.getWebXmlParam("pdaweb.mysqlDBPrefix");
        core_mysqlUser=(String) FacesUtil.getWebXmlParam("pdaweb.mysqlUser");
        core_mysqlParams= StringUtils.noNull((String) FacesUtil.getWebXmlParam("pdaweb.mysqlParams")).replaceAll("$", "&");
        String tmp = (String) FacesUtil.getWebXmlParam("pdaweb.mysqlPwd");
        tmp = StringUtils.noNull(tmp).trim();
        core_mysqlPasswd= tmp;
        osName = System.getProperty("os.name");
       
        //Initialize main database it is initialized with the PREFIX database name
        //Then we switch to the current year using setCatalog(dbThisYear);
        startFitxesDB();
   
        //And constructs the configMap from database
        //This has been updated since now reads info from the PREFIX database
        //and not from year to year
        readDatabaseCfg();
        
        lastLogins = new ArrayList<String>();
        
        initializeSGD();
              
    }
 
//    public void initializeAnuncis(ICfg iesClient)
//    {
      //Carrega tots els templates dels tipus d'anuncis definits
//        definedAnuncis = new ArrayList<AnuncisDefinition>();
//        definedAnuncis.add(new AnuncisDefinition(0,"Extraescolar","","sortida.gif",iesClient));     
//        definedAnuncis.add(new AnuncisDefinition(1,"Reunió","","reunio.gif",iesClient));
//        definedAnuncis.add(new AnuncisDefinition(2,"Event","","event.gif",iesClient));
//        definedAnuncis.add(new AnuncisDefinition(3,"Novetats","","new.gif",iesClient));
//        definedAnuncis.add(new AnuncisDefinition(4,"TIC","","pda.gif",iesClient));
        //Add any other desired type here
        //These are already defined in client -> AnuncisDefinition.getMapDefined()
        
        //Define all available icons
//        resourceIcons = new String[]{
//            "sortida.gif",
//            "reunio.gif",
//            "event.gif",
//            "new.gif",
//            "pda.gif",
//            "misc.gif",
//            "weib.gif",
//            "write.png",
//            "pdaweb.png",
//            "bulb.gif"                
//        };
//        
//    }
            
    
    private void initializeSGD()
    {
          //Initialize sgd databases
        getMysql().setCatalog(core_mysqlDB);
        
        sgd = new MyDatabase(coreDB_sgdHost, coreDB_sgdDB, 
                             coreDB_sgdUser, coreDB_sgdPasswd, coreDB_sgdParams);
        
        boolean q2 = getSgd().connect();
        
        //Initialize sgd module
        //sgd7.base.SgdBase.setMysql(getMysql());
        //sgd7.base.SgdBase.setSgd(getSgd());
       
    }
    
     /**
      * updated to read from the new common database
      * Creates the map containing values of the configuration table in db        
      */
    public void readDatabaseCfg() {

        //Populate with default values (this is already done in  ICoreData
//        configTableMap.put("anyIniciCurs", 2012);
//        configTableMap.put("sgdDBPrefix", "curso");
//        configTableMap.put("sgdUser", "root");
//        configTableMap.put("sgdPasswd", "");
//        configTableMap.put("sgdHost", "localhost");
//        configTableMap.put("versionDB", "3.1");
//        configTableMap.put("minVersion", "3.0");
//        configTableMap.put("maxDiesAntelacio", 15);
//        configTableMap.put("refreshTime", 360);
//        configTableMap.put("adminPwd", "");
//        configTableMap.put("allowWebAdmin", false);
//        configTableMap.put("prefPwd", "");
//        configTableMap.put("guardPwd", "");
//        configTableMap.put("idObservAcumulAL", -1);
//        configTableMap.put("pdawebSU", "");
//        configTableMap.put("simbolCastigDimecres", "CD");
//        configTableMap.put("sgdEnableLogger", false);
//        configTableMap.put("simbolAmonLleu", "AL");
//        configTableMap.put("simbolAmonLleuHist", "ALH");
//        configTableMap.put("simbolAmonGreu", "AG");
//        configTableMap.put("simbolExpulsio", "EX");
//        configTableMap.put("hardJustificacio", false);
//        configTableMap.put("enableRegSystemInfo", false);
        
        
        if( getMysql().isClosed() ) {
            return;
        }  //do nothing if there is no connection
        


         String SQL = "SELECT property, value, castTo FROM `"+core_mysqlDBPrefix+"`.configuracio";
         try {
               Statement st = getMysql().createStatement();
               ResultSet rs = getMysql().getResultSet(SQL,st);
                while (rs!=null && rs.next()) {
                   String str = StringUtils.noNull(rs.getString("value")).trim();
                   Object obj;
                   String type= StringUtils.noNull(rs.getString("castTo")).trim();
                   if(type.equalsIgnoreCase("int"))
                   {
                       int parsed = Integer.parseInt(str);
                       obj = new Integer(parsed);
                   }
                   else if(type.equalsIgnoreCase("Boolean"))
                   {
                       if(str.equalsIgnoreCase("true") || str.equalsIgnoreCase("yes") || str.equalsIgnoreCase("y")) {
                           obj = true;
                       }
                       else {
                           obj = false;
                       }
                   }
                   else
                   {
                       obj = str;
                   }
                   
                   configTableMap.put(rs.getString("property"), obj);                    
                }
                if(rs!=null) {
                 rs.close();
                 st.close();
             }
         } catch (SQLException ex) {
               Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
         }

         //From now on choose the correct database as default
         if(configTableMap!=null &&  configTableMap.containsKey("anyIniciCurs"))
         {
             
            //Create Alias for the most used variables
             
            coreDB_sgdHost = (String) configTableMap.get("sgdHost");               
            coreDB_sgdUser = (String) configTableMap.get("sgdUser");               
            coreDB_sgdPasswd = (String) configTableMap.get("sgdPasswd"); 
            coreDB_sgdParams = (String) configTableMap.get("sgdDBParams"); 
            coreDB_sgdDB = (String) configTableMap.get("sgdDBPrefix")+ configTableMap.get("anyIniciCurs"); 
            core_pwdSU =  (String) configTableMap.get("adminPwd");    
            
            core_pwdPREF = (String) configTableMap.get("prefPwd");    
            core_pwdGUARD = (String) configTableMap.get("guardPwd");    
             
            int any = (Integer) configTableMap.get("anyIniciCurs");
            core_mysqlDB = core_mysqlDBPrefix + any;
            
            getMysql().setCatalog(core_mysqlDB);
            anyAcademic = any;
 
        }
         
         
    }

    
     public void updateDatabaseCfg(String property, String value) 
     {
         String SQL1 = "UPDATE `"+core_mysqlDBPrefix+"`.`configuracio` SET value=? WHERE property=?";
         getMysql().preparedUpdate(SQL1, new Object[]{value,property});
     }

     public void updateDatabaseCfg() {
         for(String property: configTableMap.keySet())
         {
             updateDatabaseCfg(property, (String) configTableMap.get(property));
         }
     }

 
    public void close()
    {
        //Release databases
        if(getMysql()!=null) {
            getMysql().close();
        }
        if(getSgd()!=null) {
            getSgd().close();
        }
        //Release sgd
        SgdBase.close();
    }

    public void resetConnection() {
            
        if(getMysql()!=null) {
            getMysql().close();
        }
        if(getSgd()!=null) {
            getSgd().close();
        }
        
        //Initialize main database
        mysql =  new MyDatabase(core_mysqlHost, core_mysqlDB, 
                                core_mysqlUser, core_mysqlPasswd, core_mysqlParams);
        boolean q1 = getMysql().connect();
        
        readDatabaseCfg();
        
        
        //Initialize sgd databases
        sgd = new MyDatabase(coreDB_sgdHost, coreDB_sgdDB, 
                             coreDB_sgdUser, coreDB_sgdPasswd, coreDB_sgdParams);
        boolean q2 = getSgd().connect();
        
        //Initialize sgd module
     
    }

 
    public void startFitxesDB() {
        mysql =  new MyDatabase(core_mysqlHost, core_mysqlDBPrefix, 
                                core_mysqlUser, core_mysqlPasswd, core_mysqlParams);
        boolean q1 = getMysql().connect();
    }

    
    private HashMap<String,HashMap> generateFestiusJSON()
    {
      
        HashMap<String,HashMap> json = new HashMap<String,HashMap>();

        for(int i=0; i<getFestiusFrom().size(); i++)
        {
            Calendar calfin = Calendar.getInstance();
            calfin.setTime(getFestiusTo().get(i));
            
            Calendar calini = Calendar.getInstance();
            calini.setTime(getFestiusFrom().get(i));
            
            while(calini.compareTo(calfin)<=0)
            {
                
                String year2 = "'"+calini.get(Calendar.YEAR) +"'";
                String month2 = "'"+(calini.get(Calendar.MONTH)+1)+"'";
                String day2 = "'"+calini.get(Calendar.DAY_OF_MONTH)+"'";
                
                HashMap<String, HashMap> currentYear = null;
                if(!json.containsKey(year2))
                {
                    currentYear = new HashMap<String, HashMap>();
                    json.put(year2, currentYear);
                }
                else
                {
                    currentYear = json.get(year2);
                }
                
                HashMap<String, String> currentMonth = null;
                if(!currentYear.containsKey(month2))
                {
                    currentMonth = new HashMap<String, String>();
                    currentYear.put(month2, currentMonth);
                }
                else
                {
                    currentMonth = currentYear.get(month2);
                }
                currentMonth.put(day2, "'f'");
                  
                calini.add(Calendar.DAY_OF_MONTH, 1);
            }
            
        }
    
        //String result = json.toString().replaceAll("=",":");
        return  json;
    }

    public MyDatabase getMysql() {
        return mysql;
    }

    public MyDatabase getSgd() {
        return sgd;
    }
    
    public void reset()
    {
        id = 0;
        actions = new ArrayList<String>();
        ip = "";
    }
    
    public void inStamp(String userAbrev)
    {
        ip = remoteAddr;
        
        if(id>0) {
            return;
        } //assegura que només es crida un pic a la subrutina

        String SQL1 = "INSERT INTO sig_log (usua, ip, netbios, domain, tasca, inici) VALUES(?,?,?,?,?,NOW())";
        Object[] obj = new Object[]{userAbrev, remoteAddr, remoteHost, "", "PDAWEB"};
        id = mysql.preparedUpdateID(SQL1, obj);
    }


   public int outStamp()
   {
        if(id==0) {
           return -1;
       } 

        String SQL1 = "UPDATE sig_log SET fi=NOW(), resultat=? where id=?";
        String txt = ip+": "+actions.toString();
        Object[] obj = new Object[]{txt, id};
        int nup = mysql.preparedUpdate(SQL1, obj);
     
        return nup;
        //id=0;//assegura que només es crida un pic a la subrutina
    }

 
    public void addAction(String string) {
       if(!actions.contains(string)) {
            actions.add(string);
        }
    }

    public java.sql.Time[] getHoresClase() {
        if(horesClase==null)
        {
            loadHores();
        }
        return horesClase;
    }

    public java.sql.Time[] getHoresClase_fi() {
        if(horesClase_fi==null)
        {
            loadHores();
        }
        return horesClase_fi;
    }
    
    private void loadHores()
    {
            horesClase = new java.sql.Time[17];
            horesClase_fi = new java.sql.Time[17];

            // SQL query hores de les classses
            // idTipoHoras=1 serien hores de pati (no les empram);

             String SQL = "SELECT * FROM  `" + core_mysqlDB + "`.sig_hores_classe WHERE idTipoHoras=2 ORDER BY codigo asc";
             int i = 0;
             try {
                 Statement st = getMysql().createStatement();
                 ResultSet rs = getMysql().getResultSet(SQL, st);
                 while (rs != null && rs.next()) {
                     horesClase[i] = rs.getTime("inicio");
                     horesClase_fi[i] = rs.getTime("fin");
                     i += 1;
                 }
                 if (rs != null) {
                     rs.close();
                     st.close();
                 }
             } catch (SQLException ex) {
                 Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
             }
           
    }

    public ArrayList<java.sql.Date> getFestiusFrom() {
        if(festiusFrom==null)
        {
            loadFestius();
        }
        return festiusFrom;
    }

    public ArrayList<java.sql.Date> getFestiusTo() {
        if(festiusTo==null)
        {
            loadFestius();
        }
        return festiusTo;
    }
    
    private void loadFestius()
    {
           festiusFrom = new ArrayList<java.sql.Date>();
           festiusTo = new ArrayList<java.sql.Date>();
            

            String SQL = "SELECT * FROM `"+core_mysqlDB+"`.sig_festius ORDER BY desde";
             try {
                Statement st = getMysql().createStatement();
                ResultSet rs = getMysql().getResultSet(SQL,st);
                while (rs!=null && rs.next()) {
                    getFestiusFrom().add(rs.getDate("desde"));
                    getFestiusTo().add(rs.getDate("fins"));
                }
                if(rs!=null) {
                    rs.close();
                    st.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            
            }
          
    }

    public HashMap<String, HashMap> getFestiusJSON() {
        if(festiusJSON==null)
        {
             //Genera l'estructura JSON pels dies festius
            festiusJSON = generateFestiusJSON();
        }
        return festiusJSON;
    }

}
