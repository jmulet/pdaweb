/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.fitxes.reports;

import org.iesapp.core.util.Client;
import org.iesapp.fitxes.beans.BeanEntrevistaPares;
import org.iesapp.fitxes.beans.BeanEquipDocent;
import org.iesapp.fitxes.beans.BeanLlistaContrassenyes;
import org.iesapp.fitxes.beans.BeanNovaEntrevista;
import org.iesapp.fitxes.beans.BeanReportActuacions;
import org.iesapp.fitxes.beans.BeanResumFitxes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.iesapp.clients.iesdigital.ICoreData;
import org.iesapp.clients.iesdigital.fitxes.BeanDadesPersonals;
import org.iesapp.clients.iesdigital.fitxes.BeanFitxaCurs;
import org.iesapp.clients.iesdigital.fitxes.BeanOrla;
import org.iesapp.clients.iesdigital.missatgeria.BeanMissatge;
import org.iesapp.clients.sgd7.evaluaciones.EvaluacionesCollection;
import org.iesapp.clients.sgd7.reports.BeanSGDResumInc;
import org.iesapp.clients.sgd7.reports.InformesSGD;
import org.iesapp.database.MyDatabase;
import org.iesapp.util.DataCtrl;
import org.iesapp.util.StringUtils;

/**
 *
 * @author Josep
 */
public class ReportingClass {
    private final Client client;
    
    public ReportingClass(Client client)
    {
        this.client = client;
    }
    
    public List getList_ResumFitxes(List nExps)
    {
        List aux2 = new ArrayList();

        for(int i=0; i<nExps.size(); i++)
        {
            int expd = ((Number) nExps.get(i)).intValue();
            BeanDadesPersonals pBean = new BeanDadesPersonals(client.getIesClient());
            pBean.getFromDB(expd, client.getIesClient().getICoreData().anyAcademic);

            ArrayList<Integer> anys = BeanFitxaCurs.getAnys(expd, client.getIesClient().getMysql());
            
            for(int j=0; j<anys.size(); j++)
            {
                String anyacademic = anys.get(j)+"-"+(anys.get(j)+1);
                BeanFitxaCurs bean= new BeanFitxaCurs(client.getIesClient());
                bean.getFromDB(expd, anyacademic);

                BeanResumFitxes mybean = new BeanResumFitxes();

                //create mybean used by Jasper
                String nomcomplet = pBean.getLlinatge1() + " " + pBean.getLlinatge2() + ", " + pBean.getNom1();
                mybean.setNomcomplet(nomcomplet);
                mybean.setAny(bean.getAny_academic());
                String curscomplet = bean.getCurs()+ "-"+ bean.getGrup();
                mybean.setCurs(curscomplet);
                mybean.setNota(""+bean.getNotaMitjanaFinal());
                mybean.setNsuspeses(""+bean.getNumMateriesSuspesesJuny());
                String agtotal = "" + bean.getImportacioSGD().get("AG").getNTotal();
                mybean.setAg(agtotal);
                String altotal = "" + bean.getImportacioSGD().get("AL").getNTotal();
                mybean.setAl(altotal);

                //cal abreujar el nom del tutor (eliminam el segon llinatge)
                String tutor2 = bean.getProfessor();
                String nom = StringUtils.AfterLast(tutor2, ",").trim();
                String apellidos = StringUtils.BeforeLast(tutor2, ",").trim();
                String apellido1 = StringUtils.BeforeLast(apellidos, " ").trim();
                if(apellido1.length()==0) {
                    apellido1 = apellidos;
                }
                tutor2 = apellido1+", "+nom;

                mybean.setTutor(tutor2);
                String faltes  = "" + ( bean.getImportacioSGD().get("FA").getNTotal() );
                String faltesJ = "" + ( bean.getImportacioSGD().get("FJ").getNTotal() );

                mybean.setF(faltes);
                mybean.setFj(faltesJ);

                String nota = bean.getNotaMitjanaFinal()+"";
                mybean.setNota(nota);
                String nsus =""+bean.getNumMateriesSuspesesJuny();
                mybean.setNsuspeses(nsus);

                mybean.setCursactual(pBean.getEnsenyament()+", "+pBean.getEstudis()+", "+pBean.getGrupLletra());

                aux2.add(mybean);
            }

        }
        return aux2;
    }

    public  List getList_Contrasenyes(ArrayList expds) {

        List aux2 = new ArrayList();

        StringBuilder conditionExpds = new StringBuilder();
        for (int i = 0; i < expds.size(); i++) {
            if(conditionExpds.length()!=0) {
                conditionExpds.append(",");
            }
            conditionExpds.append(expds.get(i));
        }

            StringBuilder SQL1 = new StringBuilder(" SELECT xh.Exp2, Llinatge1, Llinatge2, Nom1, CONCAT(Estudis, ' ', Grup) AS grupo, pwd  ");
            SQL1.append(" FROM `").append(ICoreData.core_mysqlDBPrefix).append("`.xes_alumne AS xal ");
            SQL1.append(" INNER JOIN `").append(ICoreData.core_mysqlDBPrefix).append("`.xes_alumne_historic AS xh  ");
            SQL1.append(" ON xal.Exp2 = xh.Exp2   WHERE xh.AnyAcademic = '").append(client.getIesClient().getCoreCfg().anyAcademic);
            SQL1.append("'  AND xh.Exp2 IN (").append(conditionExpds).append(") ");


            try {
                Statement st = client.getIesClient().getMysql().createStatement();
                ResultSet rs1 =  client.getIesClient().getMysql().getResultSet(SQL1.toString(),st);
                
                while (rs1 != null && rs1.next()) {
                    BeanLlistaContrassenyes mybean = new BeanLlistaContrassenyes();
                    mybean.setLlinatge1(rs1.getString("Llinatge1"));
                    mybean.setLlinatge2(rs1.getString("Llinatge2"));
                    mybean.setNom1(rs1.getString("Nom1"));
                    mybean.setUsuari(rs1.getString("Exp2"));
                    mybean.setPwd(rs1.getString("pwd"));
                    mybean.setCurs(rs1.getString("grupo"));
                    aux2.add(mybean);
                }
                if (rs1 != null) {
                    rs1.close();
                    st.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportingClass.class.getName()).log(Level.SEVERE, null, ex);
            }


        
        return aux2;
    }


    public List getList_Orles(List nExps) {
        List aux2 = new ArrayList();

        String any = StringUtils.anyAcademic();
       
        String oldgrup ="";
        int myi = 0;
        int mida = nExps.size();
        while(myi<mida)
        {
             BeanOrla mybean = new BeanOrla();
            //1a columna
                boolean include = true;
                int expd = ((Number) nExps.get(myi)).intValue();
                BeanDadesPersonals pBean = new BeanDadesPersonals(client.getIesClient());
                pBean.getFromDB(expd, client.getIesClient().getCoreCfg().anyAcademic);
                BeanFitxaCurs b1 = new BeanFitxaCurs(client.getIesClient());
                b1.getFromDB(expd, any);
                String grup = pBean.getEstudis()+" "+pBean.getGrupLletra();
                
                oldgrup = grup;    
                mybean.setGrup(grup);
                mybean.setNom1(pBean.getLlinatge1()+" "+pBean.getLlinatge2()+", "+ pBean.getNom1());
                mybean.setExp1(expd+"");
                mybean.setPhoto1(pBean.getPhoto());
                mybean.setTutor(pBean.getProfTutor());
                myi += 1;

            //2a columna
                if(myi<mida)
                {
                expd = ((Number) nExps.get(myi)).intValue();
                pBean = new BeanDadesPersonals(client.getIesClient());
                pBean.getFromDB(expd, client.getIesClient().getCoreCfg().anyAcademic);
                b1 = new BeanFitxaCurs(client.getIesClient());
                b1.getFromDB(expd, any);
                grup = pBean.getEstudis()+" "+pBean.getGrupLletra();
                if(!grup.equals(oldgrup))
                {
                    include=false;
                }
                if(include)
                {
                    mybean.setGrup(grup);
                    mybean.setNom2(pBean.getLlinatge1()+" "+pBean.getLlinatge2()+", "+ pBean.getNom1());
                    mybean.setExp2(expd+"");
                    mybean.setPhoto2(pBean.getPhoto());
                    mybean.setTutor(pBean.getProfTutor());
                    myi += 1;
                }
                }

                //3a columna
                 if(myi<mida)
                {
                expd = ((Number) nExps.get(myi)).intValue();
                pBean = new BeanDadesPersonals(client.getIesClient());
                pBean.getFromDB(expd, client.getIesClient().getCoreCfg().anyAcademic);
                b1 = new BeanFitxaCurs(client.getIesClient());
                b1.getFromDB(expd, any);
                grup = pBean.getEstudis()+" "+pBean.getGrupLletra();
                if(!grup.equals(oldgrup))
                {
                    include=false;
                }
                if(include)
                {
                    mybean.setGrup(grup);
                    mybean.setNom3(pBean.getLlinatge1()+" "+pBean.getLlinatge2()+", "+ pBean.getNom1());
                    mybean.setExp3(expd+"");
                    mybean.setPhoto3(pBean.getPhoto());
                    mybean.setTutor(pBean.getProfTutor());
                    myi += 1;
                }
                }
                //4a columna
                if(myi<mida)
                {
                expd = ((Number) nExps.get(myi)).intValue();
                pBean = new BeanDadesPersonals(client.getIesClient());
                pBean.getFromDB(expd, client.getIesClient().getCoreCfg().anyAcademic);
                b1 = new BeanFitxaCurs(client.getIesClient());
                b1.getFromDB(expd, any);
                grup = pBean.getEstudis()+" "+pBean.getGrupLletra();
                if(!grup.equals(oldgrup))
                {
                    include=false;
                }
                if(include)
                {
                    mybean.setGrup(grup);
                    mybean.setNom4(pBean.getLlinatge1()+" "+pBean.getLlinatge2()+", "+ pBean.getNom1());
                    mybean.setExp4(expd+"");
                    mybean.setPhoto4(pBean.getPhoto());
                    mybean.setTutor(pBean.getProfTutor());
                    myi += 1;
                }
                }
                //5a columna
                 if(myi<mida)
                {
                expd = ((Number) nExps.get(myi)).intValue();
                pBean = new BeanDadesPersonals(client.getIesClient());
                pBean.getFromDB(expd, client.getIesClient().getCoreCfg().anyAcademic);
                b1 = new BeanFitxaCurs(client.getIesClient());
                b1.getFromDB(expd, any);
                grup = pBean.getEstudis()+" "+pBean.getGrupLletra();
                if(!grup.equals(oldgrup))
                {
                    include=false;
                }
                if(include)
                {
                    mybean.setGrup(grup);
                    mybean.setNom5(pBean.getLlinatge1()+" "+pBean.getLlinatge2()+", "+ pBean.getNom1());
                    mybean.setExp5(expd+"");
                    mybean.setPhoto5(pBean.getPhoto());
                    mybean.setTutor(pBean.getProfTutor());
                    myi += 1;
                }
                }
                //6a columna
                 if(myi<mida)
                {
                expd = ((Number) nExps.get(myi)).intValue();
                pBean = new BeanDadesPersonals(client.getIesClient());
                pBean.getFromDB(expd, client.getIesClient().getCoreCfg().anyAcademic);
                b1 = new BeanFitxaCurs(client.getIesClient());
                b1.getFromDB(expd, any);
                grup = pBean.getEstudis()+" "+pBean.getGrupLletra();
                if(!grup.equals(oldgrup))
                {
                    include=false;
                }
                if(include)
                {
                    mybean.setGrup(grup);
                    mybean.setNom6(pBean.getLlinatge1()+" "+pBean.getLlinatge2()+", "+ pBean.getNom1());
                    mybean.setExp6(expd+"");
                    mybean.setPhoto6(pBean.getPhoto());
                    mybean.setTutor(pBean.getProfTutor());
                    myi += 1;
                }
                }
           
            aux2.add(mybean);
        }

        return aux2;
    }


/**
 * Entrevista amb pares. Afegeix aquests camps a posteriori...
 *  map.put("alumne", nombre);
 *  map.put("grup", grupo);
 *  map.put("tutor", tutor);
 *  map.put("actuacionsPendents", tp);       
 * @param idEntrevista
 * @param mysql
 * @return 
 */
    
    public  ArrayList<BeanEntrevistaPares> getListEntrevistaPares(int idEntrevista, HashMap map)
    {
        //Primera passa és determinar si es tracta d'una sol.licitud antiga
        //(no s'envia res a sig_missatgeria) o es nova (hi ha les sol.licitus a missatgeria)
        boolean modelNou = false;
        String SQL1 = "SELECT tut.exp2, tut.dia, mis.id, tut.acords FROM tuta_entrevistes AS tut LEFT JOIN sig_missatgeria AS mis ON mis.idEntrevista=tut.id WHERE tut.id="+idEntrevista;
       
        int expd = 0;
        java.sql.Date diaEntrevista = null;
        String acords = null;
         try{
            Statement st = client.getIesClient().getMysql().createStatement();
            ResultSet rs1 =  client.getIesClient().getMysql().getResultSet(SQL1,st);
            if(rs1!=null && rs1.next())
            {
              expd = rs1.getInt(1);
              diaEntrevista = rs1.getDate(2);
              if(rs1.getString(3)!=null) {
                    modelNou = true;
                }
              acords = StringUtils.noNull(rs1.getString(4));
            }
            if(rs1!=null){
                rs1.close();
                st.close();
            }
        
        } catch (SQLException ex) {
            Logger.getLogger(ReportingClass.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ArrayList<BeanEntrevistaPares> listbean = new ArrayList<BeanEntrevistaPares>();
        
        /**
         * Model Nou
         */
        if(modelNou)
        {
           
                SQL1 = "SELECT mis.*, prof.nombre, prof.idSGD FROM sig_missatgeria AS mis INNER JOIN sig_professorat AS prof ON prof.abrev=mis.destinatari WHERE idEntrevista='"+idEntrevista+"' ORDER BY materia";
                //System.out.println(SQL1);
                 try {
                    Statement st = client.getIesClient().getMysql().createStatement();
                    ResultSet rs1 =  client.getIesClient().getMysql().getResultSet(SQL1,st);
                    while(rs1 != null && rs1.next()) {
                        
                        int idGrupAsig = rs1.getInt("idMateria");
                        int idProfe = rs1.getInt("idSGD");
                        BeanEntrevistaPares bean = new BeanEntrevistaPares();
                        bean.setMateria(rs1.getString("materia"));
                        bean.setProfesor(rs1.getString("nombre"));
                        bean.setActitud(StringUtils.noNull(rs1.getString("actitud")));
                        bean.setFeina(StringUtils.noNull(rs1.getString("feina")));
                        bean.setNotes(StringUtils.noNull(rs1.getString("notes")));
                        bean.setObservacions(StringUtils.noNull(rs1.getString("comentaris")));
                        if (rs1.getDate("dataContestat") != null) {
                            bean.setContestat("Sí");
                        }
                        else
                        {
                            bean.setContestat("No");
                        }

                        //Si l'informe és tipus nou, comprova si el professor ha contestat o no
                        //Si no ha contestat proporciona informacio d'emergència si existeix
                        if (bean.getContestat().equalsIgnoreCase("No")) {
                            
                            BeanMissatge auto = getAutoMissatgeria( client.getIesClient().getMysql(), client.getIesClient().getSgd(), expd, idGrupAsig, idProfe);
                            bean.setActitud(auto.getActitud());
                            bean.setFeina(auto.getFeina());
                            bean.setObservacions(auto.getComentari());
                            bean.setNotes(auto.getNotes());
                    }
                        listbean.add(bean);
                    }  
                    
                    if (rs1 != null) {
                        rs1.close();
                        st.close();
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ReportingClass.class.getName()).log(Level.SEVERE, null, ex);
                }
                    
        }
        else  //MODEL ANTIC (mostra informació d'emergencia)
        {
            //Necessit saber l'equip docent
            ArrayList<BeanEquipDocent> equipdocent = new BeanNovaEntrevista(expd, client.getIesClient().getMysql(), client.getIesClient().getSgd()).getEquipdocent();
            for(BeanEquipDocent equip: equipdocent)
            {
                 BeanEntrevistaPares bean = new BeanEntrevistaPares();
                 bean.setMateria(equip.getMateria());
                 bean.setProfesor(equip.getNombre());
                 bean.setContestat("No");
                 
                 BeanMissatge auto = getAutoMissatgeria( client.getIesClient().getMysql(), client.getIesClient().getSgd(), expd, equip.getIdgrupasig(), equip.getId());
                 bean.setActitud(auto.getActitud());
                 bean.setFeina(auto.getFeina());
                 bean.setObservacions(auto.getComentari());
                 bean.setNotes(auto.getNotes());
                   
                 listbean.add(bean);
            }
            equipdocent.clear();
            equipdocent=null;
            
         }
        
            
      
        
        //Genera l'informe               
         String dia = new DataCtrl(diaEntrevista).getDiaMesComplet();                 
         map.put("data", dia);         
         map.put("acords", acords);

         //inclou dates
         String datainici = "01/09/"+client.getIesClient().getCoreCfg().anyAcademic;
         String datafinal = new DataCtrl().getDiaMesComplet();
         String iniSQL = client.getIesClient().getCoreCfg().anyAcademic+"-09-01";
         String fiSQL = new DataCtrl().getDataSQL();
         map.put("datainici", datainici);
         map.put("datafinal", datafinal);
    
         
         //Genera l'historial d'actuacions realitzades
              ArrayList<BeanReportActuacions> list = new ArrayList<BeanReportActuacions>();

             SQL1 = " SELECT  "
                     + " CONCAT(xh.Estudis, ' ', xh.Grup) AS grup,  "
                     + " tut.exp2,  "
                     + " CONCAT(  "
                     + "   xes.Llinatge1,  "
                     + "   ' ',  "
                     + "    xes.Llinatge2,  "
                     + "    ', ',  "
                     + "    xes.Nom1  "
                     + "  ) AS alumne,  "
                     + "  CASE WHEN (ISNULL(prof.nombre))   "
                     + "  THEN tut.iniciatper   "
                     + "  ELSE prof.nombre END  "
                     + "  AS propietari,  "
                     + "  act.actuacio,  "
                     + "  tut.data1,  "
                     + "  tut.data2,  "
                     + "  tut.resolucio   "
                     + " FROM  "
                     + " tuta_reg_actuacions AS tut   "
                     + "  LEFT JOIN  "
                     + "  sig_professorat AS prof   "
                     + "  ON prof.abrev = tut.iniciatper   "
                     + "  INNER JOIN `"+ICoreData.core_mysqlDBPrefix+"`.xes_alumne AS xes   "
                     + "  ON xes.Exp2 = tut.exp2   "
                     + "  INNER JOIN `"+ICoreData.core_mysqlDBPrefix+"`.xes_alumne_historic AS xh "
                     + "  ON xes.Exp2 = xh.Exp2 AND AnyAcademic='" + client.getIesClient().getCoreCfg().anyAcademic + "'"
                     + "  INNER JOIN  "
                     + "  tuta_actuacions AS act   "
                     + "  ON act.id = tut.idActuacio   "
                     + "  AND tut.exp2="+expd
                     + "  AND tut.data1>='"+iniSQL+"' AND tut.data1<='"+fiSQL+"' "
                     + "  ORDER BY data1 ASC";

             //System.out.println(SQL1);
             
              try {
                 Statement st = client.getIesClient().getMysql().createStatement();
                 ResultSet rs1 =  client.getIesClient().getMysql().getResultSet(SQL1,st);
                 while (rs1 != null && rs1.next()) {
                     
                    BeanReportActuacions bra =  new BeanReportActuacions();
                    
                    bra.setActuacio(rs1.getString("actuacio"));
                    bra.setAlumne(rs1.getString("alumne"));
                    bra.setExpd(rs1.getString("exp2"));
                    java.sql.Date fi = rs1.getDate("data2");
                    String fitxt = "";
                    if(fi!=null) {
                    fitxt = new DataCtrl(fi).getDiaMesComplet();
                }
                    bra.setFi(fitxt);
                    bra.setInici(new DataCtrl(rs1.getDate("data1")).getDiaMesComplet());
                    bra.setPrefectura(rs1.getString("resolucio"));
                    bra.setPropietari(rs1.getString("propietari"));
                    bra.setGrup(rs1.getString("grup"));

                    list.add(bra);
                 }
                 if(rs1!=null) {
                     rs1.close();
                     st.close();
                 }
             } catch (SQLException ex) {
                 Logger.getLogger(ReportingClass.class.getName()).log(Level.SEVERE, null, ex);
             }
             JRBeanCollectionDataSource db2 = new JRBeanCollectionDataSource(list);
             map.put("subReport",db2);
        
         
         return listbean;
    }



     // Retorna els comentaris positius o negatius de l'alumne  
    public BeanMissatge getAutoMissatgeria(MyDatabase mysql, MyDatabase sgd, int expedient, int idGrupAsig, int idProfe) {
         BeanMissatge sms = new BeanMissatge();
         sms.setNotes(client.getIesClient().getMissatgeriaCollection().getAutoNotes(expedient, idGrupAsig, idProfe));
                 
         InformesSGD infsgd = new InformesSGD(client.getSgdClient()); 
         BeanSGDResumInc inf = infsgd.getResumIncidenciesByGrupAsig(expedient, EvaluacionesCollection.getInicioCurso(client.getSgdClient()),
                                                            new java.util.Date(), idGrupAsig);
         
         if(inf.getAg()==0 && inf.getAl()==0)
         {
             sms.setActitud("No té amonestacions.");
         }
         else
         {
             String txt = "Té "+inf.getAg()+" Amon. Greus i "+inf.getAl()+" Amon. Lleus. ";
             txt +="Recents: "+infsgd.getLastIncidenciesByGrupAsig(expedient, 4, idGrupAsig, Arrays.asList(new String[]{"AG", "AL"}));
             sms.setActitud(txt);
         }
         
         String cncp = "";
         if(inf.getCp()!=0 || inf.getCn()!=0)
         {
             cncp += infsgd.getLastIncidenciesByGrupAsig(expedient, 4, idGrupAsig, Arrays.asList(new String[]{"CP", "CN"}));
         }
         sms.setComentari(cncp);
         
         String feina="";
         if(inf.getFj()>0)
         {
            feina = "Faltes sense justificar = "+inf.getFa()+ ", justificades = "+inf.getFj();
         }
         else if(inf.getFa()>0)
         {
             feina="Té "+inf.getFa()+" faltes sense justificar ";
         }
         sms.setFeina(feina);
         
         infsgd = null;
         inf = null;
         
         return sms;
    }

}
