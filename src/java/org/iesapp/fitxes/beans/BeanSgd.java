/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.fitxes.beans;

import java.util.ArrayList;
import java.util.Date;
import javax.faces.model.SelectItem;
import org.iesapp.clients.sgd7.base.BeanTipoIncidencias;

/**
 *
 * @author Josep
 */
public class BeanSgd {

    private String tipusInforme="1";
    private Date fromDate=new Date();
    private Date toDate=new Date();
    private boolean convivencia = true;
    private boolean assistencia = false;
    private boolean puntualitat= false;
    private String fromAlumne = "";
    private String toAlumne = "";
    private SelectItem[] listAlumnes;
    private BeanTipoIncidencias[] selectedIncidencies;
    private ArrayList<BeanTipoIncidencias> listIncidencies;
    
    
    public String getTipusInforme() {
        return tipusInforme;
    }

    public void setTipusInforme(String tipusInforme) {
        this.tipusInforme = tipusInforme;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public boolean isConvivencia() {
        return convivencia;
    }

    public void setConvivencia(boolean convivencia) {
        this.convivencia = convivencia;
    }

    public boolean isAssistencia() {
        return assistencia;
    }

    public void setAssistencia(boolean assistencia) {
        this.assistencia = assistencia;
    }

    public boolean isPuntualitat() {
        return puntualitat;
    }

    public void setPuntualitat(boolean puntualitat) {
        this.puntualitat = puntualitat;
    }

    public String getFromAlumne() {
        return fromAlumne;
    }

    public void setFromAlumne(String fromAlumne) {
        this.fromAlumne = fromAlumne;
    }

    public String getToAlumne() {
        return toAlumne;
    }

    public void setToAlumne(String toAlumne) {
        this.toAlumne = toAlumne;
    }

    public SelectItem[] getListAlumnes() {
        return listAlumnes;
    }

    public void setListAlumnes(SelectItem[] listAlumnes) {
        this.listAlumnes = listAlumnes;
    }


    public ArrayList<BeanTipoIncidencias> getListIncidencies() {
        return listIncidencies;
    }

    public void setListIncidencies(ArrayList<BeanTipoIncidencias> listIncidencies) {
        this.listIncidencies = listIncidencies;
    }

    public void setSelectedIncidencies(BeanTipoIncidencias[] selectedIncidencies) {
        this.selectedIncidencies = selectedIncidencies;
    }

    public BeanTipoIncidencias[] getSelectedIncidencies() {
        return selectedIncidencies;
    }

 
 
   
}
