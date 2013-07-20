/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.beans;

/**
 *
 * @author Josep
 */
public class BeanColors {
    protected String htmlColor = "";
    protected String nom = "";

    
    public BeanColors(String nom, String hex)
    {
        this.nom = nom;
        this.htmlColor = hex;
    }
    /**
     * @return the htmlColor
     */
    public String getHtmlColor() {
        return htmlColor;
    }

    /**
     * @param htmlColor the htmlColor to set
     */
    public void setHtmlColor(String htmlColor) {
        this.htmlColor = htmlColor;
    }

    /**
     * @return the nom
     */
    public String getNom() {
        return nom;
    }

    /**
     * @param nom the nom to set
     */
    public void setNom(String nom) {
        this.nom = nom;
    }
}
