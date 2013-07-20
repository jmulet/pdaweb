/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.iesapp.database.MyDatabase;
import org.iesapp.util.StringUtils;
import org.iesapp.web.cloudws.FacesUtil;

/**
 *
 * @author Josep
 */

public class Preferences implements java.io.Serializable {
    
    public static final byte PERSONALPDA=0;
    public static final byte GUARDIAPDA=1;
    //these are the default values
    
    //Aparença    
    private String currentTheme;
    private int tipusPDA;
    private String pp_theme = "aristo";     //Default theme for personal PDA
    private String pg_theme = "eggplant";   //Default theme for guardia PDA
    private String locale = "es";
    private int pp_fontscale=90; //in %
    private int pp_menutype=2;
    private int pg_menutype=2;
    private boolean pg_showPictAlumnes=true;
    private boolean pp_showPictAlumnes=true;
    
    
    //Comportament
    private int pp_timeout = 2;
    private int pp_startPage = 1;
    
    //Bases de dades
    private MyDatabase mysql;
    private String abrev;
    
     
    public Preferences()
    {
      //Default constructor
        this.mysql = null;
        this.abrev = "";
        String tmp = (String) FacesUtil.getWebXmlParam("pdaweb.sessionTimeout");  
        pp_timeout = (int) Double.parseDouble(tmp);
        
        String tmp2 = (String) FacesUtil.getWebXmlParam("pdaweb.defaultThemePP");          
        if(tmp2!=null && !tmp2.trim().isEmpty()) {
            pp_theme = tmp2;
        }
        
        String tmp3 = (String) FacesUtil.getWebXmlParam("pdaweb.defaultThemePG");          
        if(tmp3!=null && !tmp3.trim().isEmpty()) {
            pg_theme = tmp3;
        }
        
        String tmp4 = (String) FacesUtil.getWebXmlParam("pdaweb.defaultLocale");          
        if(tmp4!=null && !tmp4.trim().isEmpty()) {
            locale = tmp4;
        }
    }
    
    public void loadPreferences(MyDatabase mysql, String abrev)
    {
        this.mysql = mysql;
        this.abrev = abrev;
        load();
    }
    
    
    private HashMap<String, Object> getAsMap()
    {
        HashMap<String,Object> map = new HashMap<String,Object>();
        
        map.put("pp.theme", getPp_theme());
        map.put("pp.locale", locale);
        map.put("pp.fontscale", getPp_fontscale()+"");
        map.put("pp.menutype", getPp_menutype()+"");
        map.put("pp.showPictAlumnes", isPp_showPictAlumnes()?"S":"N");        
        map.put("pp.startPage", getPp_startPage()+"");
        
        map.put("pg.theme", getPg_theme());
        map.put("pg.locale", locale);
        map.put("pg.fontscale", getPp_fontscale()+"");
        map.put("pg.menutype", getPg_menutype()+"");
        map.put("pg.showPictAlumnes", isPp_showPictAlumnes()?"S":"N");        
        
        return map;
    }
    
    public void reset() {
           setPp_theme("aristo");
           String tmp2 = (String) FacesUtil.getWebXmlParam("pdaweb.defaultThemePP");          
           if(tmp2!=null && !tmp2.trim().isEmpty()) {
            setPp_theme(tmp2);
        }
            
           tmp2 = (String) FacesUtil.getWebXmlParam("pdaweb.defaultThemePG");          
           if(tmp2!=null && !tmp2.trim().isEmpty()) {
            setPg_theme(tmp2);
        }
           
           setPp_fontscale(90); //in %
           setPp_menutype(1);
           setPp_showPictAlumnes(true);
       
           setPp_startPage(1);
    }
    
    
    public void save()
    {
        String SQL1 = "UPDATE usu_usuari SET preferences_pdaweb=? WHERE Nom='"+this.abrev+"'";
        Object[] obj = new Object[]{StringUtils.HashToString(this.getAsMap(), ";")};
        int nup = mysql.preparedUpdate(SQL1, obj);
        //System.out.println(nup+"   SQL1  "+SQL1);
    }
     
     private void load()
    {
        String SQL1 = "SELECT preferences_pdaweb FROM usu_usuari WHERE Nom='"+this.abrev+"'";
       
        
        String txtmap = "";
        
        try {
            Statement st = mysql.createStatement();
            ResultSet rs1 = mysql.getResultSet(SQL1,st);     
            while(rs1!=null && rs1.next())
            {
               txtmap = StringUtils.noNull( rs1.getString("preferences_pdaweb") );
            }
            if(rs1!=null) {
                rs1.close();
                st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(!txtmap.isEmpty())
        {
            HashMap<String,String> map = StringUtils.StringToHash(txtmap, ";");
            for(String ky: map.keySet())
            {
                if(ky.equals("pp.theme"))
                {
                    this.setPp_theme(map.get(ky));
                }
                else if(ky.equals("pg.theme"))
                {
                    this.setPg_theme(map.get(ky));
                }
                if(ky.equals("pp.locale"))
                {
                    this.locale = map.get(ky);
                }
                else if(ky.equals("pp.fontscale"))
                {
                     this.setPp_fontscale((int) Double.parseDouble( map.get(ky) ));
                }
                else if(ky.equals("pp.menutype"))
                {
                     this.setPp_menutype((int) Double.parseDouble( map.get(ky) ));
                }  
                else if(ky.equals("pp.showPictAlumnes"))
                {
                     this.setPp_showPictAlumnes(map.get(ky).equalsIgnoreCase("S"));
                }  
                else if(ky.equals("pp.startPage"))
                {
                     this.setPp_startPage((int) Double.parseDouble( map.get(ky) ));
                }      
            }
        }
            
    }
     
     public String getPaginaInicia() {
        String page="";
        
        switch(getPp_startPage())
        {
                            
            case 0: page="preferences";break;
            case 1: page="passarllista";break;                
            case 2: page="activitats";break;
            case 3: page="missatges";break;
            case 4: page="missatgeria";break;
            case 5: page="reserves";break;                
            default:  page="passarllista";
        }
        
        return page;
    }
 

    /**
     * @return the pp_fontscale
     */
    public int getPp_fontscale() {
        return pp_fontscale;
    }

    /**
     * @param pp_fontscale the pp_fontscale to set
     */
    public void setPp_fontscale(int pp_fontscale) {
        this.pp_fontscale = pp_fontscale;
    }

    /**
     * @return the pp_menutype
     */
    public int getPp_menutype() {
        return pp_menutype;
    }

    /**
     * @param pp_menutype the pp_menutype to set
     */
    public void setPp_menutype(int pp_menutype) {
        this.pp_menutype = pp_menutype;
    }

    /**
     * @return the pp_showPictAlumnes
     */
    public boolean isPp_showPictAlumnes() {
        return pp_showPictAlumnes;
    }

    /**
     * @param pp_showPictAlumnes the pp_showPictAlumnes to set
     */
    public void setPp_showPictAlumnes(boolean pp_showPictAlumnes) {
        this.pp_showPictAlumnes = pp_showPictAlumnes;
    }

    /**
     * @return the pp_timeout
     */
    public int getPp_timeout() {
        return pp_timeout;
    }

    /**
     * @param pp_timeout the pp_timeout to set
     */
    public void setPp_timeout(int pp_timeout) {
        this.pp_timeout = pp_timeout;
    }

    /**
     * @return the pp_startPage
     */
    public int getPp_startPage() {
        return pp_startPage;
    }

    /**
     * @param pp_startPage the pp_startPage to set
     */
    public void setPp_startPage(int pp_startPage) {
        this.pp_startPage = pp_startPage;
    }

    /**
     * @return the theme
     */
    public String getTheme() {
        return getPp_theme();
    }

    /**
     * @param theme the theme to set
     */ 
    public void setTheme(String theme) {
        this.setPp_theme(theme);
    }

    /**
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCurrentTheme() {
        
        if(getTipusPDA()==GUARDIAPDA)
        {
            currentTheme = getPg_theme();
        }
        else
        {
            currentTheme = getPp_theme();
        }
        
        return currentTheme;
    }

    public int getTipusPDA() {
        return tipusPDA;
    }

    public void setTipusPDA(int tipusPDA) {
        this.tipusPDA = tipusPDA;
    }

    public int getPg_menutype() {
        return pg_menutype;
    }

    public void setPg_menutype(int pg_menutype) {
        this.pg_menutype = pg_menutype;
    }

    public String getPp_theme() {
        return pp_theme;
    }

    public void setPp_theme(String pp_theme) {
        this.pp_theme = pp_theme;
    }

    public String getPg_theme() {
        return pg_theme;
    }

    public void setPg_theme(String pg_theme) {
        this.pg_theme = pg_theme;
    }

    public boolean isPg_showPictAlumnes() {
        return pg_showPictAlumnes;
    }

    public void setPg_showPictAlumnes(boolean pg_showPictAlumnes) {
        this.pg_showPictAlumnes = pg_showPictAlumnes;
    }
 

 
  
}
                    
    
