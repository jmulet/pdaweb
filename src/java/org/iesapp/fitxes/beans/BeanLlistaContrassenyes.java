/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iesapp.fitxes.beans;

/**
 *
 * @author Josep
 */
public class BeanLlistaContrassenyes {
    public BeanLlistaContrassenyes()
    {

    }

    private String llinatge1="";
    private String llinatge2="";
    private String nom1="";
    private String curs="";
    private String usuari="";
    private String pwd="";

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }


    public String getUsuari() {
        return usuari;
    }

    public void setUsuari(String usuari) {
        this.usuari = usuari;
    }


    public String getCurs() {
        return curs;
    }

    public void setCurs(String curs) {
        this.curs = curs;
    }


    public String getNom1() {
        return nom1;
    }

    public void setNom1(String nom1) {
        this.nom1 = nom1;
    }


    public String getLlinatge2() {
        return llinatge2;
    }

    public void setLlinatge2(String llinatge2) {
        this.llinatge2 = llinatge2;
    }


    public String getLlinatge1() {
        return llinatge1;
    }

    public void setLlinatge1(String llinatge1) {
        this.llinatge1 = llinatge1;
    }

}
