/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.fitxes.beans;

/**
 *
 * @author Josep
 */
public class BeanReportActuacions {
    
    private String expd;
    private String alumne;
    private String grup;
    private String propietari;
    private String actuacio;
    private String inici;
    private String fi;
    private String prefectura;

    
    public String getPrefectura() {
        return prefectura;
    }

   
    public void setPrefectura(String prefectura) {
        this.prefectura = prefectura;
    }


    public String getFi() {
        return fi;
    }

    public void setFi(String fi) {
        this.fi = fi;
    }


    public String getInici() {
        return inici;
    }

    public void setInici(String inici) {
        this.inici = inici;
    }


    public String getActuacio() {
        return actuacio;
    }

    public void setActuacio(String actuacio) {
        this.actuacio = actuacio;
    }


    public String getPropietari() {
        return propietari;
    }

    public void setPropietari(String propietari) {
        this.propietari = propietari;
    }


    public String getGrup() {
        return grup;
    }

    public void setGrup(String grup) {
        this.grup = grup;
    }


    public String getAlumne() {
        return alumne;
    }

    public void setAlumne(String alumne) {
        this.alumne = alumne;
    }


    public String getExpd() {
        return expd;
    }

    public void setExpd(String expd) {
        this.expd = expd;
    }

    
}
