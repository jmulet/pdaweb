/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.fitxes.beans;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.iesapp.database.MyDatabase;

/**
 *
 * @author Josep
 */
public class BeanEntrevista {
    
    private int id;
    private Date fecha;
    private String solicitat;
    private String contestat;
    private String acords;
    private boolean sms = false;
    protected String instruccions="";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getSolicitat() {
        return solicitat;
    }

    public void setSolicitat(String solicitat) {
        this.solicitat = solicitat;
    }

    public String getContestat() {
        return contestat;
    }

    public void setContestat(String contestat) {
        this.contestat = contestat;
    }
   
    public boolean isSms() {
        return sms;
    }

    public void setSms(boolean sms) {
        this.sms = sms;
    }

   public static ArrayList<BeanEntrevista> getListEntrevistes(int expd, MyDatabase mysql) {
        ArrayList<BeanEntrevista> list = new ArrayList<BeanEntrevista>();
        
        String SQL1 = "SELECT ent.id, ent.abrev, ent.dia, ent.para, ent.sms, ent.acords, "
                + " CONCAT( SUM(IF(dataContestat IS NULL,0,1)), ' de ',"
                + " SUM(IF(mis.destinatari IS NULL,0,1))) AS score FROM tuta_entrevistes "
                + " AS ent LEFT JOIN sig_missatgeria AS mis ON mis.idEntrevista=ent.id  "
                + " WHERE exp2='" + expd + "' GROUP BY ent.id ORDER BY ent.dia DESC";
        
      
        
        try {
            Statement st = mysql.createStatement();
            ResultSet rs1 = mysql.getResultSet(SQL1,st);
            while (rs1 != null && rs1.next()) {
                
                
                BeanEntrevista bean = new BeanEntrevista();
                
                bean.id = rs1.getInt("id");                
                bean.fecha = new java.util.Date(rs1.getDate("dia").getTime());
                bean.sms = rs1.getInt("sms") > 0;
                String rawPara = rs1.getString("para");
                ParseFieldPara pfp = new ParseFieldPara(rawPara);
                String para = pfp.getText(mysql);
                bean.solicitat = para;                
                bean.contestat = rs1.getString("score");
                bean.acords = rs1.getString("acords");
                
                
                list.add(bean);
            }
            if (rs1 != null) {
                rs1.close();
                st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(BeanEntrevista.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return list;
    }

    public String getAcords() {
        return acords;
    }

    public void setAcords(String acords) {
        this.acords = acords;
    }

    public String getInstruccions() {
        return instruccions;
    }

    public void setInstruccions(String instruccions) {
        this.instruccions = instruccions;
    }
}
