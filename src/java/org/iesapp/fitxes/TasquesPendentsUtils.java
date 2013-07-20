/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.fitxes;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.iesapp.clients.iesdigital.IClient;
import org.iesapp.clients.iesdigital.fitxes.TasquesPendents;
import org.iesapp.clients.sgd7.alumnos.BeanAlumnoTutoria;

/**
 *
 * @author Josep
 */
public class TasquesPendentsUtils
{
  
    
    //Implement tasques pendents
    //Given an input array of alumnes returns the same array updated with tasques pendents
    public static ArrayList<BeanAlumnoTutoria> loadTasquesPendents(ArrayList<BeanAlumnoTutoria> bh, String abrevTutor, IClient client)
    {
        TasquesPendents tp = new TasquesPendents(client);
        tp.checkTasquesPendentsForeground();
        //////System.out.println("tasques pendents per a l'any "+iesClient.getCoreCfg().anyAcademic);
        //Ara comprova si té entrevistes pendents
        HashMap<Integer,String> entrevistesPendents = new HashMap<Integer,String>();
        String SQL1 = "SELECT exp2, DATE_FORMAT(MAX(TENV.DIA), '%d/%m/%Y') AS entrevistes FROM tuta_entrevistes AS tenv WHERE abrev='"+abrevTutor+"' AND TENV.dia>=CURRENT_DATE() GROUP BY EXP2";
       
        try {
            Statement st = client.getMysql().createStatement();
            ResultSet rs1 = client.getMysql().getResultSet(SQL1,st);
            while(rs1!=null && rs1.next())
            {
                entrevistesPendents.put(rs1.getInt("exp2"), rs1.getString("entrevistes"));
            }
            if(rs1!=null) {
                rs1.close();
                st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TasquesPendents.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        for(int i=0; i<bh.size(); i++)
        {
               int expd = bh.get(i).getExpediente();
               ////System.out.println(" checking tp of alumne..... "+expd);
        
               //tp.checkTasquesPendents(expd);
               
               if(tp.jobs.containsKey(expd))
               {
                      bh.get(i).setMsgPendents( tp.jobs.get(expd).detallTasks.toString() );
                      bh.get(i).setAccionsStatus( BeanAlumnoTutoria.RED_FLAG );
                }
                else
                {
                    //no te tasques pendents
                    if(tp.oberts.contains(expd))
                    {
                        bh.get(i).setMsgPendents("Té actuacions sense tancar" );   
                        bh.get(i).setAccionsStatus( BeanAlumnoTutoria.ORANGE_FLAG );
                    }
                    else
                    {
                        bh.get(i).setAccionsStatus( BeanAlumnoTutoria.GREEN_FLAG );
                    }
                }
               
               
               if(entrevistesPendents.containsKey(expd))
               {
                    bh.get(i).setEntrevistesPendents(entrevistesPendents.get(expd));
               }
                              
       }   
       return bh;
    }

}

