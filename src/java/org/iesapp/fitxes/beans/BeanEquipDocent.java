/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.fitxes.beans;

/**
 *
 * @author Josep
 */
public class BeanEquipDocent {
    protected int ordre;   //--> unique id (ordering)
    private int id;      //--> Id professor
    private String abrev;
    private String nombre;
    private String materia;
    private int idasig;
    private int idgrupasig;
     
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }

    public String getAbrev() {
        return abrev;
    }

    public void setAbrev(String abrev) {
        this.abrev = abrev;
    }

    public int getIdasig() {
        return idasig;
    }

    public void setIdasig(int idasig) {
        this.idasig = idasig;
    }

    public int getIdgrupasig() {
        return idgrupasig;
    }

    public void setIdgrupasig(int idgrupasig) {
        this.idgrupasig = idgrupasig;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }
}
