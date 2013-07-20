/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iesapp.web.pdaweb.data;

  
import org.iesapp.core.util.Client;
import org.iesapp.core.util.CoreCfg;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.iesapp.clients.sgd7.evaluaciones.BeanEvaluaciones;
import org.iesapp.clients.sgd7.incidencias.BeanIncidencias;
import org.iesapp.database.MyDatabase;
import org.iesapp.util.DataCtrl;
import org.iesapp.web.pdaweb.beans.StudentStat;

/** 
 *
 * @author Josep
 */
public class Data {
    private final Client client;
    
    public Data(Client client)
    {
        this.client = client;
    }
    
    public java.io.InputStream getPhoto(MyDatabase mysql, int expedient) {
          
         java.io.InputStream photo=null;
         String SQL2 = "Select Foto from `"+CoreCfg.core_mysqlDBPrefix+"`.xes_alumne_detall where Exp_FK_ID='"+expedient+"'";
           
            try {
                Statement st = mysql.createStatement();
                ResultSet rs2 =  mysql.getResultSet(SQL2,st);
                while( rs2!=null && rs2.next() )
                { 
               
                  photo = rs2.getBinaryStream("Foto");
               
                }
                if(rs2!=null) {
                    rs2.close();
                    st.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return photo;
    }

/**
 * Obté els totals del nombre d'incidencies FA, FJ, AL, AG
 * en cada avaluacio d'aquest alumne dins d'un grup i professor
 * @param idAlumno
 * @param idGrupAsig
 * @param idProfesor
 * @return 
 */
    public StudentStat getStudentStat(final int idAlumno, final int idGrupAsig, final String idProfesor)
    {
        StudentStat studentStat = new StudentStat();
        ArrayList<BeanEvaluaciones> evaluaciones = client.getSgdClient().getEvaluacionesCollection().getEvaluaciones();
        for(int i=0; i<evaluaciones.size();i++)
        {
            String desde = new DataCtrl(evaluaciones.get(i).getFechaInicio()).getDataSQL();
            String hasta = new DataCtrl(evaluaciones.get(i).getFechaFin()).getDataSQL();
            ArrayList<BeanIncidencias> incidencias = client.getSgdClient().getIncidenciasCollection().getIncidencias(idAlumno, idGrupAsig, idProfesor, desde, hasta);
            int fa = 0;
            int fj = 0;
            int al = 0;
            int ag = 0;
            for(int j=0; j<incidencias.size(); j++)
            {
                BeanIncidencias inc = incidencias.get(j);
                if(inc.getSimbolo().equalsIgnoreCase("FA") || inc.getSimbolo().equalsIgnoreCase("F") ) {
                    fa += 1;
                }
                else if(inc.getSimbolo().equalsIgnoreCase("FJ") ) {
                    fj += 1;
                }
                else if(inc.getSimbolo().equalsIgnoreCase("AL") ) {
                    al += 1;
                }
                else if(inc.getSimbolo().equalsIgnoreCase("AG") ) {
                    ag += 1;
                }
            }
            
            if(evaluaciones.get(i).getValorExportable().contains("1a"))
            {
                studentStat.setFa1(fa);
                studentStat.setFj1(fj);
                studentStat.setAl1(al);
                studentStat.setAg1(ag);
            }
            else if(evaluaciones.get(i).getValorExportable().contains("2a"))
            {
                studentStat.setFa2(fa);
                studentStat.setFj2(fj);
                studentStat.setAl2(al);
                studentStat.setAg2(ag);
            }
            else if(evaluaciones.get(i).getValorExportable().contains("3a") ||
                    evaluaciones.get(i).getValorExportable().contains("Ord"))
            {
                studentStat.setFa3(fa);
                studentStat.setFj3(fj);
                studentStat.setAl3(al);
                studentStat.setAg3(ag);
            }
           
        }
        
        return studentStat;
    }
}
