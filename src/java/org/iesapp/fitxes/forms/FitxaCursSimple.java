/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iesapp.fitxes.forms;

import org.iesapp.core.util.CoreCfg;
import org.iesapp.core.util.IesClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Josep
 */
public class FitxaCursSimple extends BeanFitxaCursSimple {

    private final int nexp;
    private final IesClient iesClient;
   
    public FitxaCursSimple(IesClient iesClient, int nexp)
    {
        this.iesClient = iesClient;
        this.nexp = nexp;
    }

    
    public void getFromDB(String anyAcademic)
    {

         String SQL1 = "select * from `"+CoreCfg.core_mysqlDBPrefix+"`.fitxa_alumne_curs where Exp_FK_ID="+nexp+
                " AND Any_academic='"+anyAcademic.trim()+"'";
         try {
            Statement st =iesClient.getMysql().createStatement();
            ResultSet rs1 =iesClient.getMysql().getResultSet(SQL1,st); 
            while (rs1.next()) {

                // CARREGA AQUI LES PROPIETATS DE LA BASE DE DADES

            curs = rs1.getString("Estudis");
            grup=rs1.getString("Grup");
            any_academic=rs1.getString("Any_academic");
            nivell=rs1.getString("Ensenyament");
            professor=rs1.getString("Professor");
          }
          rs1.close();
          st.close();
        } catch (SQLException ex) {
            Logger.getLogger(FitxaCursSimple.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public ArrayList<String> getAnys()
    {
        ArrayList<String> list = new ArrayList<String>();

         String SQL1 = "select Any_academic from `"+CoreCfg.core_mysqlDBPrefix+"`.fitxa_alumne_curs where Exp_FK_ID="+nexp;
         try {
            Statement st = iesClient.getMysql().createStatement();
            ResultSet rs1 = iesClient.getMysql().getResultSet(SQL1,st); 
            while (rs1!=null && rs1.next()) {
               list.add( rs1.getString("Any_academic") );
            }
            rs1.close();
            st.close();
        } catch (SQLException ex) {
            Logger.getLogger(FitxaCursSimple.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return list;
    }
    
    public ArrayList<BeanFitxaCursSimple> getInfoAnys()
    {
        ArrayList<BeanFitxaCursSimple> list = new ArrayList<BeanFitxaCursSimple>();

        String SQL1 = "SELECT * FROM `"+CoreCfg.core_mysqlDBPrefix+"`.fitxa_alumne_curs WHERE Exp_FK_ID="+nexp+
                " ORDER BY Any_academic";
         try {
            Statement st = iesClient.getMysql().createStatement();
            ResultSet rs1 = iesClient.getMysql().getResultSet(SQL1,st); 
            while (rs1.next()) {
                
            BeanFitxaCursSimple bean = new BeanFitxaCursSimple();
            
            bean.curs = rs1.getString("Estudis");
            bean.grup=rs1.getString("Grup");
            bean.any_academic=rs1.getString("Any_academic");
            bean.nivell=rs1.getString("Ensenyament");
            bean.professor=rs1.getString("Professor");
            
            list.add(bean);
          }
          rs1.close();
          st.close();
        } catch (SQLException ex) {
            Logger.getLogger(FitxaCursSimple.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        return list;
    }

}
