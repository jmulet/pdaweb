/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.iesapp.fitxes.reports;

/**
 *
 * @author Josep:: Cal sempre cridar aquesta subrutina per tal d'unificar
 *                 tots els diferents convenis de maneres d'escriure els
 *                 grups. El getMethods() et dona una forma universal que
 *                 s'ha d'utilitzar en tot en programa per tal d'evitar
 *                 problemes de compatibilitat.
 *
 *  Possibles valors que retorna la subrutina::
 *      xEnsenyament = ESO, BAT, ...
 *      xEstudis     = 1R ESO, 2N ESO, 3R ESO, 4T ESO, 1R BATX, 2N BATX, ...
 *      xGrup        = A, B, C ...
 *
 *      kNivell      = ESO, BATX, PQPI, MITJA, SUP
 *      kCursInt (integer) = 1, 2, 3, 4, ....
 *      kGrup        = A, B, C ... (el mateix que xGrup)
 */
public class Grup {

    public static final int XESTIB = 0;
    public static final int KRONOWIN = 1;

    public Grup()
    {
        defaultConstructor();
    }



    public Grup(int mode, String ensenyament, String estudis, String grup)
    {
    
        if(mode == XESTIB)          //ENSENYAMENT, ESTUDIS, GRUP
        {
            xEnsenyament = ensenyament.toUpperCase();
            if(xEnsenyament.contains("ESO")) {
                xEnsenyament = "ESO";
            }
            else if(xEnsenyament.contains("BAT")) {
                xEnsenyament = "BAT";
            }

            xEstudis = estudis.trim().toUpperCase();
            if(xEstudis.contains("ESO")) {
                xEstudis = xEstudis.substring(0,2) + " ESO";
            }
            else if(xEstudis.toUpperCase().contains("BAT")) {
                xEstudis = xEstudis.substring(0,2) + " BATX";
            }

            xGrup = grup.toUpperCase().trim();

            //Construim en format Kronowin
            kGrup = xGrup;

            if(xEnsenyament.equals("ESO"))
            {
                kNivell = "ESO";
                if(xEstudis.contains("ESO"))
                {
                     kCursInt = (int) Double.parseDouble(xEstudis.substring(0,1));

                }
                else if(xEstudis.contains("HOT"))
                {
                    kCursInt = 1;
                    kNivell ="PQPI";
                }
                else if(xEstudis.contains("PQPI"))
                {
                    kCursInt = 2;
                    kNivell ="PQPI";
                }
            }
            else if(xEnsenyament.equals("BAT"))
            {
                kNivell = "BATX";
                kCursInt = (int) Double.parseDouble(xEstudis.substring(0,1));
            }



        }
        else if(mode == KRONOWIN)   //NIVELL, CURS, GRUP
        {
            System.out.println("ERRORR!!!!!!!!!!!!!!!!!!!! NO IMPLEMENTAT!!!!!!!!!");
        }
        else
        {
            defaultConstructor();
        }
    }



    public Grup(String nivel, int curso, String grupo)
    {
        kNivell = nivel.toUpperCase().trim();
        kCursInt = curso;
        kGrup = grupo.toUpperCase().trim();

        //Construim ara format XESTIB
        xGrup = kGrup;
        if(kNivell.equals("ESO"))
        {
            xEnsenyament = "ESO";
            xEstudis = kCursInt + terminacio(kCursInt)+" ESO";
        }
        else if(kNivell.equals("BATX"))
        {
            xEnsenyament = "BAT";
            xEstudis = kCursInt + terminacio(kCursInt)+" BAT";
        }

    }

    

    private void defaultConstructor()
    {
        //camps que utilitza el xestib
        xEnsenyament = "";
        xEstudis = "";
        xGrup = "";

        //camps que utlitza el kronowin o programa de signatures
        kNivell = "";
        kCursInt = 0;
        kGrup = "";
    }


    private String  terminacio(int k)
    {
        String term="";
        switch(k)
        {
            case (1): term = "R"; break;
            case (2): term = "N"; break;
            case (3): term = "R"; break;
            case (4): term = "T"; break;
        }
        return term;
    }

    private String xEnsenyament="";
    private String xEstudis="";
    private String xGrup="";
    private String kNivell="";
    private String kGrup="";
    private int kCursInt=0;

    public void print()
    {
        System.out.println("xEnsenyament="+xEnsenyament+",xEstudis="+xEstudis+",xGrup="+xGrup+
               ",kNivell="+kNivell+",kGrup="+kGrup+",kCursInt="+kCursInt );
    }

    public int getKCursInt() {
        return kCursInt;
    }

    public void setKCursInt(int kCursInt) {
        this.kCursInt = kCursInt;
    }


    public String getKGrup() {
        return kGrup;
    }

    public void setKGrup(String kGrup) {
        this.kGrup = kGrup;
    }


    public String getKNivell() {
        return kNivell;
    }

    public void setKNivell(String kNivell) {
        this.kNivell = kNivell;
    }


    public String getXGrup() {
        return xGrup;
    }

    public void setXGrup(String xGrup) {
        this.xGrup = xGrup;
    }


    public String getXEstudis() {
        return xEstudis;
    }

    public void setXEstudis(String xEstudis) {
        this.xEstudis = xEstudis;
    }


    public String getXEnsenyament() {
        return xEnsenyament;
    }

    public void setXEnsenyament(String xEnsenyament) {
        this.xEnsenyament = xEnsenyament;
    }

}
