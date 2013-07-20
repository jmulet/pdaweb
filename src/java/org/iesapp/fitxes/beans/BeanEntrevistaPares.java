/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iesapp.fitxes.beans;

/**
 *
 * @author Josep
 */
public class BeanEntrevistaPares {

    private String profesor="";
    private String materia="";
    private String actitud="";
    private String feina="";
    private String notes="";
    private String observacions="";
    private String contestat="";

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }


    public String getProfesor() {
        return profesor;
    }

    public void setProfesor(String profesor) {
        this.profesor = profesor;
    }

    /**
     * @return the actitud
     */
    public String getActitud() {
        return actitud;
    }

    /**
     * @param actitud the actitud to set
     */
    public void setActitud(String actitud) {
        this.actitud = actitud;
    }

    /**
     * @return the feina
     */
    public String getFeina() {
        return feina;
    }

    /**
     * @param feina the feina to set
     */
    public void setFeina(String feina) {
        this.feina = feina;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * @return the observacions
     */
    public String getObservacions() {
        return observacions;
    }

    /**
     * @param observacions the observacions to set
     */
    public void setObservacions(String observacions) {
        this.observacions = observacions;
    }

    /**
     * @return the contestat
     */
    public String getContestat() {
        return contestat;
    }

    /**
     * @param contestat the contestat to set
     */
    public void setContestat(String contestat) {
        this.contestat = contestat;
    }

}
