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
import org.iesapp.clients.sgd7.profesores.ProfesoresCollection;
import org.iesapp.database.MyDatabase;

/**
 *
 * @author Josep
 */
public class BeanNovaEntrevista {

    private Date dia;
    private ArrayList<BeanEquipDocent> equipdocent;
    private BeanEquipDocent[] selectedProfes;
    private boolean sendsms = true;
    private boolean contestaOnline = true;
    private String sms = "";
    protected String instruccions = "";
    
    public BeanNovaEntrevista() {
        equipdocent = new ArrayList<BeanEquipDocent>();
        //Default constructor
    }

    //carrega l'equip docent de l'alumne amb n. d'expedient expd
    public BeanNovaEntrevista(int expd, MyDatabase mysql, MyDatabase sgd) {
        equipdocent = new ArrayList<BeanEquipDocent>();
        selectedProfes = null;
        dia = null;
        sendsms = true;


        //Aquesta query dona les assignatures dels alumnes
        String SQL1 = "SELECT DISTINCT alumn.expediente, alumn.nombre, g.grupo, a.descripcion, "
                + " a.id as idAsig, ga.id as idGrupAsig, "
                + "p.codigo AS profesor, p.nombre AS NombreProfe "
                + " FROM Asignaturas a "
                + " INNER JOIN ClasesDetalle cd ON 1=1 "
                + " INNER JOIN HorasCentro hc ON 1=1 "
                + " INNER JOIN Horarios h ON 1=1 "
                + " INNER JOIN GrupAsig ga ON 1=1 "
                + " INNER JOIN Grupos g ON 1=1 "
                + " INNER JOIN AsignaturasAlumno aa ON 1=1 "
                + " INNER JOIN alumnos alumn ON alumn.id=aa.idAlumnos "
                + " LEFT OUTER JOIN Aulas au ON h.idAulas=au.id "
                + " LEFT OUTER JOIN Profesores p ON h.idProfesores=p.id "
                + "  WHERE alumn.expediente=" + expd + " AND (a.descripcion NOT LIKE 'Atenció educativa%') AND "
                + " (a.descripcion NOT LIKE 'Tut%') AND "
                + "  aa.idGrupAsig=ga.id "
                + " AND  (aa.opcion<>'0' AND (cd.opcion='X' OR cd.opcion=aa.opcion)) "
                + " AND  h.idClases=cd.idClases "
                + " AND cd.idGrupAsig=ga.id "
                + " AND  h.idHorasCentro=hc.id "
                + " AND ga.idGrupos=g.id "
                + " AND  ga.idAsignaturas=a.id "
                + " ORDER BY a.descripcion, NombreProfe ";
        
        String alumne = "";
        String grup = "";
        
        int ordre = 0;
        try {
            Statement st = sgd.createStatement();
            ResultSet rs1 = sgd.getResultSet(SQL1, st);
            while (rs1 != null && rs1.next()) {
                
                alumne = rs1.getString("nombre");
                grup = rs1.getString("grupo");
                
                BeanEquipDocent bean = new BeanEquipDocent();
                int id = rs1.getInt("profesor");
                bean.setOrdre(ordre);
                bean.setId(id);                
                bean.setNombre(rs1.getString("NombreProfe"));
                bean.setMateria(rs1.getString("descripcion"));
                bean.setIdasig(rs1.getInt("idAsig"));
                bean.setIdgrupasig(rs1.getInt("idGrupAsig"));
                bean.setAbrev(ProfesoresCollection.getAbrev(id + "", mysql));
                //System.out.println("equipdocent -> "+id+" "+bean.getIdasig()+" "+bean.getIdgrupasig()+" "+bean.getMateria());
                equipdocent.add(bean);
                ordre += 1;
            }
        } catch (SQLException ex) {
            Logger.getLogger(BeanNovaEntrevista.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        selectedProfes = equipdocent.toArray(new BeanEquipDocent[equipdocent.size()]);
        sms = "Entrant als programes fitxes o reserves, emplenau informació de l'alumne/a " + alumne + " de " + grup + " per entrevista amb pares";
    }
    
    public Date getDia() {
        return dia;
    }
    
    public void setDia(Date dia) {
        this.dia = dia;
    }
    
    public ArrayList<BeanEquipDocent> getEquipdocent() {
        return equipdocent;
    }
    
    public void setEquipdocent(ArrayList<BeanEquipDocent> equipdocent) {
        this.equipdocent = equipdocent;
    }
    
    public BeanEquipDocent[] getSelectedProfes() {
        return selectedProfes;
    }
    
    public void setSelectedProfes(BeanEquipDocent[] selectedProfes) {
        this.selectedProfes = selectedProfes;
    }
    
    public boolean isSendsms() {
        return sendsms;
    }
    
    public void setSendsms(boolean sendsms) {
        this.sendsms = sendsms;
    }
    
    public String getSms() {
        return sms;
    }
    
    public void setSms(String sms) {
        this.sms = sms;
    }
    
    public boolean isContestaOnline() {
        return contestaOnline;
    }
    
    public void setContestaOnline(boolean contestaOnline) {
        this.contestaOnline = contestaOnline;
    }

    public String getInstruccions() {
        return instruccions;
    }

    public void setInstruccions(String instruccions) {
        this.instruccions = instruccions;
    }
}
