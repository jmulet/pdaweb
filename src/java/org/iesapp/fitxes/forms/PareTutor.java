/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iesapp.fitxes.forms;

/**
 *
 * @author Josep
 */
public class PareTutor {

    public PareTutor()
    {

    }

    private String parentesc="";
    private String nom="";
    private String professio="";
    private int    edat=0;
    private String emails="";
    private String telefons="";


    public String getTelefons() {
        return telefons;
    }

    public void setTelefons(String telefons) {
        this.telefons = telefons;
    }

    public String getEmails() {
        return emails;
    }

    public void setEmails(String emails) {
        this.emails = emails;
    }


    public int getEdat() {
        return edat;
    }

    public void setEdat(int edat) {
        this.edat = edat;
    }


    public String getProfessio() {
        return professio;
    }

    public void setProfessio(String professio) {
        this.professio = professio;
    }


    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }


    public String getParentesc() {
        return parentesc;
    }

    public void setParentesc(String parentesc) {
        this.parentesc = parentesc;
    }

}
