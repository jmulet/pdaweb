/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.guardies;

import org.iesapp.core.util.IesClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.iesapp.clients.sgd7.clases.BeanClaseGuardia;
import org.iesapp.util.DataCtrl;

/**
 *
 * @author Josep
 */
public class GuardiesUtils {
       
/**
 * @param mysql
 * @param list llistat amb el beanClaseGuardia
 * @param dia
 * @param hhora és la idHora de iesdigital
 * @return 
 */     
public static ArrayList<BeanClaseGuardia> ClasesGuardiesUpdateStatus(
        ArrayList<BeanClaseGuardia> list, java.util.Date dia, int hhora, IesClient iesClient)
{
    //Desa les signatures en memoria
    HashMap<String,Integer> map = new HashMap<String,Integer>();
     

    String taula = "sig_signatures";
    if(hhora>7)
    {
        taula = "sig_signatures_tarda";
        hhora -= 7;
    }
    
    //Determina si s'han creat les signatures d'aquest dia
    String data = new DataCtrl(dia).getDataSQL();
    String SQL0 = "SELECT * FROM sig_data WHERE data='"+data+"'";
    boolean created = false;
     try {
        Statement st = iesClient.getMysql().createStatement();
        ResultSet rs0 =  iesClient.getMysql().getResultSet(SQL0,st);
        if (rs0 != null && rs0.next()) {
            created = true;            
        }
        if(rs0!=null) {
            rs0.close();
            st.close();
        }
    } catch (SQLException ex) {
        Logger.getLogger(GuardiesUtils.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    String SQL1 = "SELECT tb.h"+hhora+" as status, prof.idSGD FROM "+taula+" as tb inner join sig_professorat as prof on tb.abrev=prof.abrev WHERE data='"+data+"'";
         try {
            Statement st =  iesClient.getMysql().createStatement();
            ResultSet rs1 =  iesClient.getMysql().getResultSet(SQL1,st);
            while(rs1!=null && rs1.next())
            {
                map.put(rs1.getString("idSGD"), rs1.getInt("status"));
            }
            if(rs1!=null) {
                rs1.close();
                st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(GuardiesUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

        //System.out.println("map is : "+map.toString());
        //update
        for(int i=0; i<list.size();i++)
        {
            String id = list.get(i).getIdProfesor();
            if(map.containsKey(id))
            {
                int status = map.get(id);
                list.get(i).setStatus(status);
                if(status==0)
                {
                    list.get(i).setImg("icon01.gif"); //?
                }
                else if(status==1)
                {
                     list.get(i).setImg("icon02.gif"); //OK
                }
                else if(status==2)
                {
                     list.get(i).setImg("icon03.gif"); //F
                }
                else if(status==3)
                {
                     list.get(i).setImg("icon04.gif"); //Sortida
                }  
                else
                {
                    if (created) {
                        list.get(i).setImg("expulsat.gif"); //x
                        //Inabilita aquesta classe
                        //Es tracta d'una classe que es a sgd i falta a iesdigital
                        list.get(i).setStatus(1);  //Status 1 vol dir OK. (impideix marcar)
                    } else {
                        list.get(i).setImg("icon01.gif"); //? (no s'han creat les signatures)
                        list.get(i).setStatus(0);
                    }
                }
                    
            }
            else
            {
                 if(created)
                 {
                    list.get(i).setImg("expulsat.gif"); //x
                    //Inabilita aquesta classe
                    //Es tracta d'una classe que es a sgd i falta a iesdigital
                    list.get(i).setStatus(1);  //Status 1 vol dir OK. (impideix marcar)
                 }
                 else
                 {
                      list.get(i).setImg("icon01.gif"); //? (no s'han creat les signatures)
                      list.get(i).setStatus(0);
                 }
            }
        }
        
        map = null;
        return list;
}
}
