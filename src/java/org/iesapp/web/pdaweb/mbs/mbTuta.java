/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

 
import org.iesapp.core.util.Client;
import org.iesapp.core.util.CoreCfg;
import org.iesapp.fitxes.TasquesPendentsUtils;
import org.iesapp.fitxes.beans.BeanEntrevista;
import org.iesapp.fitxes.beans.BeanEquipDocent;
import org.iesapp.fitxes.beans.BeanNovaEntrevista;
import org.iesapp.fitxes.beans.BeanSgd;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import org.iesapp.clients.iesdigital.dates.DatesControl;
import org.iesapp.clients.sgd7.alumnos.BeanAlumnoTutoria;
import org.iesapp.clients.sgd7.base.BeanTipoIncidencias;
import org.iesapp.clients.sgd7.incidencias.BeanIncidenciasDia;
import org.iesapp.clients.sgd7.incidencias.Incidencias;
import org.iesapp.clients.sgd7.mensajes.Mensajes;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.util.DataCtrl;
import org.iesapp.util.StringUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.iesapp.web.cloudws.FacesUtil;
 
/**
 *
 * @author Josep
 */
//@Named(value = "mbTuta")
//@SessionScoped
@ManagedBean(name = "mbTuta")
@SessionScoped
//@ViewScoped
public class mbTuta implements java.io.Serializable{
    private BeanProfesor selectedUser;
    private BeanAlumnoTutoria alumnoSelected;
    private ArrayList<BeanAlumnoTutoria> llistatTuta;
    private ArrayList<BeanIncidenciasDia> incidencies;
    private BeanIncidenciasDia incidenciaSelected;
    private final HashMap<String, Integer> mapaInc2Id;
    private int pantalla = 0;
    private BeanSgd beansgd;
    private ArrayList<BeanTipoIncidencias> listTipoIncidencias;
    private String formatInformes="PDF";
    private ArrayList<BeanEntrevista> listEntrevistas;
    private BeanNovaEntrevista beanEntrevista;
    private BeanEntrevista selectedEntrevista;
    private java.util.Date dia;
    private String specialDates;
    private boolean festiu;
    private final Client client;
    
    
    public mbTuta()
    {
        
        selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser"); 
        client = (Client) FacesUtil.getSessionMapValue("client"); 
        //System.out.println("in mbTuta selectedUser "+selectedUser);
        
        //Carrega llistat d'alumnes de tutoria
        if(selectedUser!=null)
        {
        llistatTuta = client.getSgdClient().getAlumnosCollection().getAlumnosProfeTutoria(client.getIesClient().getSgd(), selectedUser);
        //Actualitza tasques pendents
        //sobre l'array list d'aquesta clase
        llistatTuta = TasquesPendentsUtils.loadTasquesPendents(llistatTuta, selectedUser.getAbrev(), client.getIesClient());
        }
        
        mapaInc2Id = client.getSgdClient().getTipoIncidencias().getMapaInc2Id();
        
        //bean per a informes client.getIesClient().getSgd()
        beansgd = new BeanSgd();
        
        //list per a entrevistes de pares
        listEntrevistas = new ArrayList<BeanEntrevista>();
        dia = new java.util.Date();
        
        //prepara nova entrevista
        beanEntrevista = new BeanNovaEntrevista();
        
        //JSON TO HIGHLIGHT DAYS IN CALENDAR
        specialDates = "{}";
    }

    
    public String loadIncidencies()
    {
         
        if(alumnoSelected==null) 
        {
            return null;                      
        }        
        festiu = new DatesControl(dia, client.getIesClient()).esFestiu();
        incidencies = client.getSgdClient().getIncidenciasCollection().getIncidenciasInDay(alumnoSelected.getId(), new String[]{"FA","FJ","F","RE","R","RJ"}, dia);       
        specialDates = client.getSgdClient().getIncidenciasCollection().getJSONDiasIncidencias(alumnoSelected.getId(),client.getIesClient().getSgd());
        return "tutoriaJustifica";
       
    }
    
    public void onLoadIncidencies(SelectEvent event) {        

         dia = (java.util.Date) event.getObject();
         festiu = new DatesControl(dia, client.getIesClient()).esFestiu();
         
         incidencies = client.getSgdClient().getIncidenciasCollection().getIncidenciasInDay(alumnoSelected.getId(), new String[]{"FA","FJ","F","RE","R","RJ"}, dia);       
         //specialDates = client.getSgdClient().getIncidenciasCollection().getJSONDiasIncidencias(alumnoSelected.getId(),client.getIesClient().getSgd());
         
    }
    
    
    public String refreshSpecialDays()
    {
        specialDates = client.getSgdClient().getIncidenciasCollection().getJSONDiasIncidencias(alumnoSelected.getId(),client.getIesClient().getSgd());
        return "tutoriaJustifica";
    }
    
    public String loadEntrevistes()
    {
        
        if (alumnoSelected == null) {
            return null;
        }
  
      listEntrevistas = BeanEntrevista.getListEntrevistes(alumnoSelected.getExpediente(), client.getIesClient().getMysql());    
  
      return "tutoriaEntrevista";
     }
    
    public void preparaNovaEntrevista()
    {
        beanEntrevista = new BeanNovaEntrevista(alumnoSelected.getExpediente(), client.getIesClient().getMysql(), client.getIesClient().getSgd());    
    }
    
    public void onChangeDiaNovaEntrevista(SelectEvent event)
    {
        beanEntrevista.setDia( (java.util.Date) event.getObject() );
        String noudia = new DatesControl(beanEntrevista.getDia(), client.getIesClient()).getDiaMesComplet();
        String sms = StringUtils.BeforeLast(beanEntrevista.getSms(), "pares");
        beanEntrevista.setSms(sms + "pares dia "+noudia+".");
    }
    
    public void onCreaNovaEntrevista()
    {
    
        if(beanEntrevista.getDia()==null)
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Atenció","Cal una data vàlida per a l'entrevista"));
            return;
        }
        
        if(beanEntrevista.getDia()!=null && new DatesControl(beanEntrevista.getDia(), client.getIesClient()).esFestiu())
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Atenció", "Heu triat un dia no lectiu per a l'entrevista"));
            return;
        }
        
        if(beanEntrevista.isContestaOnline() && 
          (beanEntrevista.getSelectedProfes()==null || beanEntrevista.getSelectedProfes().length==0) )
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Atenció", "Heu de triar algun professor de l'equip docent"));
            return;
        }
        
        String diaSQL = new DatesControl(beanEntrevista.getDia(), client.getIesClient()).getDataSQL();
        //Passa zero, comprova si ja existeix una entrevista per aquest dia
        String SQL1 = "SELECT id FROM tuta_entrevistes WHERE dia='"+diaSQL+"' AND exp2="+alumnoSelected.getExpediente();
       
        try {
            Statement st = client.getIesClient().getMysql().createStatement();
            ResultSet rs = client.getIesClient().getMysql().getResultSet(SQL1,st);
            if(rs!=null && rs.next())
            {
                 FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Atenció","Ja existeix una entrevista aquest dia"));
                 rs.close();
                 return;
            }
            if(rs!=null){
                rs.close();
                st.close();
            }       
        } catch (SQLException ex) {
            Logger.getLogger(mbTuta.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        
        //Primera passa: escriu l'entrevista a tuta_entrevistes
        StringBuilder para= new StringBuilder("[");
        ArrayList<Integer> aProfes = new ArrayList<Integer>();
        for(BeanEquipDocent bed : beanEntrevista.getSelectedProfes())
        {
            if(para.length()>1) {
                para.append(",");
            }
            para.append(bed.getAbrev());
            aProfes.add(bed.getId());
        }
        para.append("]");
        
        SQL1 = "INSERT INTO tuta_entrevistes (exp2,abrev,dia,dataEnviat,sms,para,acords,observacions)"
                + " VALUES('" + alumnoSelected.getExpediente() + "','" + selectedUser.getAbrev() + "','"
                + diaSQL + "',NOW(),'"
                + (beanEntrevista.isSendsms() ? "1" : "0") + "','" + para.toString() + "','',?)";
  
        int idEntrevista = client.getIesClient().getMysql().preparedUpdateID(SQL1, new Object[]{beanEntrevista.getInstruccions()});
        
    
        if(idEntrevista<=0)
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error","No s'ha pogut crear l'entrevista"));
            return;
        }
        //Segona passa: envia sms als professors
        //Envia els missatges a les pda's
        Mensajes smssgd = null;
        if(beanEntrevista.isSendsms())
        {
            smssgd = client.getSgdClient().getMensajes(Integer.parseInt(selectedUser.getIdProfesor()), beanEntrevista.getSms(), aProfes);
            smssgd.save();
        }

        //Tercera passa: escriu les peticions de missatgeria
        if(beanEntrevista.isContestaOnline())
        {
            int nup = 0;
            System.out.println("---------------------------------------------->>> sig_missatgeria");
            for(BeanEquipDocent bed : beanEntrevista.getSelectedProfes())
            {
                System.out.println("--> "+bed.getMateria()+" - "+bed.getAbrev());
                
                int idmensajeProfesor = 0;
                if(smssgd!=null) {
                    idmensajeProfesor = smssgd.getDestinatarios().get(bed.getId());
                }
                SQL1 = "INSERT INTO sig_missatgeria (idEntrevista, destinatari, idMateria, materia, "
                        + " idMensajeProfesor) "
                        + " VALUES('" + idEntrevista + "', '" + bed.getAbrev() + "', '"
                        + bed.getIdgrupasig() + "', '" + bed.getMateria() + "', '" + idmensajeProfesor + "' )";

                nup += client.getIesClient().getMysql().executeUpdate(SQL1);
            }

            if(nup<beanEntrevista.getSelectedProfes().length)
            {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Error","S'ha creat un error en processar l'entrevista"));
            }
        }
        
        
        RequestContext.getCurrentInstance().execute("novawv.hide()");
        if(beanEntrevista.isContestaOnline() && beanEntrevista.isSendsms())
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Molt bé","L'equip docent ha estat notificat i té la informació sobre l'entrevista"));
        }
        else if(beanEntrevista.isContestaOnline() && !beanEntrevista.isSendsms())
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Ups!","L'equip docent NO ha estat notificat però té la informació sobre l'entrevista"));
        }
        else
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Atenció!","No heu demanat informació al professorat però encara podreu obtenir informació d'emergència"));
        }
        
        
        //Torna a carregar el llistat d'entrevistes
        listEntrevistas = BeanEntrevista.getListEntrevistes(alumnoSelected.getExpediente(), client.getIesClient().getMysql());    
        
    }
    
    public void onDeleteEntrevista()
    {
        if(selectedEntrevista==null) {
            return;
        }
        
        //Esborra l'entrada a tuta_entrevistes
        int idEntrevista = selectedEntrevista.getId();
        String SQL1 = "DELETE FROM tuta_entrevistes WHERE id="+idEntrevista;
        client.getIesClient().getMysql().executeUpdate(SQL1);
        
        //Esborra els missatges dels professors
        SQL1 = "SELECT idMensajeProfesor FROM sig_missatgeria WHERE idEntrevista="+idEntrevista;
       
        try {
            Statement st = client.getIesClient().getMysql().createStatement();
            ResultSet rs1 = client.getIesClient().getMysql().getResultSet(SQL1,st);     
            if(rs1!=null && rs1.next())
            {
                org.iesapp.clients.sgd7.mensajes.MensajesProfesores mp = client.getSgdClient().getMensajesProfesores(rs1.getInt(1));
                int idMensajes = mp.getIdMensajes();
                if(idMensajes>0)
                {
                     client.getSgdClient().getMensajes(idMensajes).delete();
                }
            }
            if(rs1!=null){
                rs1.close();
                st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(mbTuta.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Esborra les entrades de missatgeria
         SQL1 = "DELETE FROM sig_missatgeria WHERE idEntrevista="+idEntrevista;
         client.getIesClient().getMysql().executeUpdate(SQL1);     
         
         //Torna a carregar les entrevistes
          listEntrevistas = BeanEntrevista.getListEntrevistes(alumnoSelected.getExpediente(), client.getIesClient().getMysql());    
    }
    
    public void switchStatus(int ordre)
    {
        BeanIncidenciasDia bean = incidencies.get(ordre);
      
        if(bean==null) {
            return;
        }
       // System.out.println("he de canviar l'estat de "+bean.getId()+" a "+bean.getSimbolo());
        
 
            
            String value = bean.getSimbolo();
            if(value==null)
            {
                return;
            }
            else if(value.trim().isEmpty())
            {
                if(bean.getId()!=0) 
                {
                    int delete = client.getSgdClient().getIncidencias(bean).delete();
                }
                
            }
            else if(value.startsWith("FA"))
            {
                if(bean.getId()==0)
                {
                    //System.out.println("we must create new FA in "+i);
                    
                    bean.setIdTipoIncidencias(mapaInc2Id.get("FA"));        
                    bean.setHora(client.getIesClient().getMysql().getServerTime().toString());
                    int save = client.getSgdClient().getIncidencias(bean).save();
                }
                else
                {
                       //System.out.println("we must switch to FA in "+i);
                       bean.setIdTipoIncidencias(mapaInc2Id.get("FA"));
                       int save = client.getSgdClient().getIncidencias(bean).save(); 
                }
            }
            else if(value.startsWith("FJ"))
            {
                if(bean.getId()==0)
                {
                    //System.out.println("we must create new FA in "+i);
                    
                    bean.setIdTipoIncidencias(mapaInc2Id.get("FJ"));        
                    bean.setHora(client.getIesClient().getMysql().getServerTime().toString());
                    int save = client.getSgdClient().getIncidencias(bean).save();
                }
                else
                {
                       //System.out.println("we must switch to FA in "+i);
                       bean.setIdTipoIncidencias(mapaInc2Id.get("FJ"));
                       Incidencias inc = client.getSgdClient().getIncidencias(bean);
                       //Si estic en mode hardJustificacio primer he d'esborrar la incidencia
                       if ((Boolean) CoreCfg.configTableMap.get("hardJustificacio")) {
                            int ndelete = inc.delete();
                            if (ndelete > 0) {
                                //La nova incidencia justificada amb la id del tutor
                                inc.setIdProfesores(selectedUser.getIdProfesor());
                            }
                        }
                       int save = inc.save(); 
                }
            }
            else if(value.startsWith("RE"))
            {
                if(bean.getId()==0)
                {
                    //System.out.println("we must create new FA in "+i);
                    
                    bean.setIdTipoIncidencias(mapaInc2Id.get("RE"));        
                    bean.setHora(client.getIesClient().getMysql().getServerTime().toString());
                    int save = client.getSgdClient().getIncidencias(bean).save();
                }
                else
                {
                       //System.out.println("we must switch to FA in "+i);
                       bean.setIdTipoIncidencias(mapaInc2Id.get("RE"));
                       int save = client.getSgdClient().getIncidencias(bean).save(); 
                }
            }
            else if(value.startsWith("RJ"))
            {
                if(bean.getId()==0)
                {
                    //System.out.println("we must create new FA in "+i);
                    
                    bean.setIdTipoIncidencias(mapaInc2Id.get("RJ"));        
                    bean.setHora(client.getIesClient().getMysql().getServerTime().toString());
                    int save = client.getSgdClient().getIncidencias(bean).save();
                }
                else
                {
                       //System.out.println("we must switch to FA in "+i);
                       bean.setIdTipoIncidencias(mapaInc2Id.get("RJ"));
                       int save = client.getSgdClient().getIncidencias(bean).save(); 
                }
        }
       //Refresh
       loadIncidencies();
       client.getIesClient().getCoreCfg().addAction("justify-tuta");
       
    }
    
    
    //GENERATE ELS REPORTS DE LA PAGINA
    public void genReportResumFitxes()
    {
        String ext = formatInformes;
        
        ArrayList expds = new ArrayList();
        for(BeanAlumnoTutoria bean: llistatTuta) {
            expds.add(bean.getExpediente());
        }
        
        byte fileType = 0;
        if(ext.equalsIgnoreCase("PDF")) {
            fileType = ReportFactory.PDF;
        }
        else if(ext.equalsIgnoreCase("XLS")) {
            fileType = ReportFactory.XLS;
        }
        else if(ext.equalsIgnoreCase("DOC")) {
            fileType = ReportFactory.DOC;
        }

        List list_ResumFitxes = client.getReportingClass().getList_ResumFitxes(expds);
        ReportFactory rf = new ReportFactory("alumnat/resumCurs", fileType);
       // rf.setContentDisposition("attachment");
        rf.setDataList(list_ResumFitxes);
        rf.setFileName("resumFitxesTutoria");
        rf.getGeneratedFile(); 
    }
    
    public void genReportLlistatContrasenyes()
    {
        String ext = formatInformes;
        
        ArrayList expds = new ArrayList();
        for(BeanAlumnoTutoria bean: llistatTuta) {
            expds.add(bean.getExpediente());
        }
        
        byte fileType = 0;
        if(ext.equalsIgnoreCase("PDF")) {
            fileType = ReportFactory.PDF;
        }
        else if(ext.equalsIgnoreCase("XLS")) {
            fileType = ReportFactory.XLS;
        }
        else if(ext.equalsIgnoreCase("DOC")) {
            fileType = ReportFactory.DOC;
        }

        List list = client.getReportingClass().getList_Contrasenyes(expds);
        ReportFactory rf = new ReportFactory("alumnat/usuarisSGD", fileType);
       // rf.setContentDisposition("attachment");
        rf.setDataList(list);
        rf.setFileName("llistaContrasenyes");
        rf.getGeneratedFile(); 
    }
    
     public void genReportOrla()
     {
        String ext = formatInformes;
        
        ArrayList expds = new ArrayList();
        for(BeanAlumnoTutoria bean: llistatTuta) {
            expds.add(bean.getExpediente());
        }
        
        byte fileType = 0;
        if(ext.equalsIgnoreCase("PDF")) {
            fileType = ReportFactory.PDF;
        }
        else if(ext.equalsIgnoreCase("XLS")) {
            fileType = ReportFactory.XLS;
        }
        else if(ext.equalsIgnoreCase("DOC")) {
            fileType = ReportFactory.DOC;
        }

        List list = client.getReportingClass().getList_Orles(expds);
        ReportFactory rf = new ReportFactory("alumnat/orla", fileType);
       // rf.setContentDisposition("attachment");
        rf.setDataList(list);
        rf.setFileName("orla");
        rf.getMap().put("cursAcademic", client.getIesClient().getCoreCfg().anyAcademic+"-"+(client.getIesClient().getCoreCfg().anyAcademic+1));
        rf.getGeneratedFile(); 
    }
     
    public void preparaInformesSGD()
    {
        beansgd = new BeanSgd();
        beansgd.setTipusInforme("1");
        SelectItem[] selitem = new SelectItem[llistatTuta.size()];
        
        for(int i=0; i<llistatTuta.size(); i++)
        {
            selitem[i] = new SelectItem(""+i, llistatTuta.get(i).getNombre());
        }
        beansgd.setListAlumnes(selitem);      
        beansgd.setFromAlumne("0");
        beansgd.setToAlumne(""+(llistatTuta.size()-1));
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.YEAR, client.getIesClient().getCoreCfg().anyAcademic );                
        beansgd.setFromDate(cal.getTime());
        cal=null;
        //Carrega les incidencies disponibles
        listTipoIncidencias = new ArrayList<BeanTipoIncidencias>(client.getSgdClient().getTipoIncidencias().getMapIncidencias().values());                        
        beansgd.setListIncidencies( listTipoIncidencias );      
        //Selecciona les incidencies de convivencia
       
        beansgd.setSelectedIncidencies( filterTipoIncidencias(new String[]{"AL","ALH","AG","AM"}) );
        
    }
    
    
    public void genReportEntrevista()
    {
        if(selectedEntrevista==null) {
            return;
        }
         
        byte fileType = ReportFactory.PDF;
        ReportFactory rf = new ReportFactory("tutoria/entrevistaParesPLUS", fileType);
        
        List list = client.getReportingClass().getListEntrevistaPares(selectedEntrevista.getId(), rf.getMap());
                
        rf.setDataList(list);
        rf.setFileName("entrevistaPares");
        //Acaba d'introduir informacio al mapa
        rf.getMap().put("alumne", alumnoSelected.getNombre());
        rf.getMap().put("grup", alumnoSelected.getGrupo());
        rf.getMap().put("tutor", alumnoSelected.getNomTutor());
        rf.getMap().put("actuacionsPendents", alumnoSelected.getMsgPendents());  
        rf.getGeneratedFile(); 
    }
    
   
    
    public void onChangeCBs()
    {
        ArrayList<String> simbols = new ArrayList<String>();
        if(beansgd.isConvivencia())
        {
            simbols.add("AL");simbols.add("ALH");simbols.add("AG");simbols.add("AM");
        }
        if(beansgd.isAssistencia())
        {
            simbols.add("FA");simbols.add("F");simbols.add("FJ");
        }
        if(beansgd.isPuntualitat())
        {
            simbols.add("RE");simbols.add("R");simbols.add("RJ");
        }
        
         beansgd.setSelectedIncidencies( filterTipoIncidencias(simbols.toArray(new String[simbols.size()])) );
    }
     
    public void onEditAcords(int id)
    {
        String acords = "";
        for(int i=0; i<listEntrevistas.size(); i++)
        {
            if(listEntrevistas.get(i).getId()==id)
            {
                acords = listEntrevistas.get(i).getAcords();
                break;
            }
        }
        String SQL1 = "UPDATE tuta_entrevistes SET acords=? WHERE id="+id;
        client.getIesClient().getMysql().preparedUpdate(SQL1, new Object[]{acords});
    }
    
    public void genInformeSGD()
    {
        String ext = formatInformes;
        
        int al0= Integer.parseInt(beansgd.getFromAlumne());
        int al1= Integer.parseInt(beansgd.getToAlumne());
        if(al1<al0)
        {
            int al00 = al0;
            al0 = al1;
            al1 = al00;
        }
        
        ArrayList<Integer> expds = new ArrayList<Integer>();
        for(int i=al0; i<=al1; i++) {
            expds.add(llistatTuta.get(i).getExpediente());
        }
        
        ArrayList<Integer> incs = new ArrayList<Integer>();
        for(BeanTipoIncidencias bean: beansgd.getSelectedIncidencies())
        {
            incs.add(bean.getId());
        }
        
        byte fileType = 0;
        if(ext.equalsIgnoreCase("PDF")) {
            fileType = ReportFactory.PDF;
        }
        else if(ext.equalsIgnoreCase("XLS")) {
            fileType = ReportFactory.XLS;
        }
        else if(ext.equalsIgnoreCase("DOC")) {
            fileType = ReportFactory.DOC;
        }

        ReportFactory rf;         
        List list;
        if(beansgd.getTipusInforme().equals("1"))
        {
                rf = new ReportFactory("alumnat/listaIncidenciasAlumnos", fileType);
                list = client.getSgdClient().getInformesSGD().getListIncidencies(expds, incs, beansgd.getFromDate(), beansgd.getToDate());   
                rf.setFileName("llistatIncidencies");
        }
        else
        {
                rf = new ReportFactory("alumnat/resumIncidenciasAlumnos", fileType);
                list = client.getSgdClient().getInformesSGD().getResumIncidencies(expds, beansgd.getFromDate(), beansgd.getToDate());    
                rf.setFileName("resumIncidencies");
        }
        
        
        rf.setDataList(list);      
        rf.getMap().put("datainici", new DataCtrl(beansgd.getFromDate()).getDiaMesComplet());
        rf.getMap().put("datafinal", new DataCtrl(beansgd.getToDate()).getDiaMesComplet());
        rf.getGeneratedFile(); 
    }

    
    public void manageSendSms()
    {
        beanEntrevista.setSendsms( beanEntrevista.isContestaOnline() );
    }
    /**
     * @return the alumnoSelected
     */
    public BeanAlumnoTutoria getAlumnoSelected() {
        return alumnoSelected;
    }

    /**
     * @param alumnoSelected the alumnoSelected to set
     */
    public void setAlumnoSelected(BeanAlumnoTutoria alumnoSelected) {
        this.alumnoSelected = alumnoSelected;
    }

    /**
     * @return the llistatTuta
     */
    public ArrayList<BeanAlumnoTutoria> getLlistatTuta() {
        return llistatTuta;
    }

    /**
     * @param llistatTuta the llistatTuta to set
     */
    public void setLlistatTuta(ArrayList<BeanAlumnoTutoria> llistatTuta) {
        this.llistatTuta = llistatTuta;
    }

    public int getPantalla() {
        return pantalla;
    }

    public void setPantalla(int pantalla) {
        this.pantalla = pantalla;
    }
 
    public BeanSgd getBeansgd() {
        return beansgd;
    }

    public void setBeansgd(BeanSgd beansgd) {
        this.beansgd = beansgd;
    }
    
    

    
     public BeanTipoIncidencias[] filterTipoIncidencias(String[] simbolos)
     {
        ArrayList<BeanTipoIncidencias> localSelected = new ArrayList<BeanTipoIncidencias>();
        List<String> wordList = Arrays.asList(simbolos);  
        for(BeanTipoIncidencias bean: listTipoIncidencias)
        {
            if(wordList.contains(bean.getSimbolo()))
            {
                localSelected.add(bean);
            }
                
        }
        return localSelected.toArray(new BeanTipoIncidencias[localSelected.size()]);
     }

    public String getFormatInformes() {
        return formatInformes;
    }

    public void setFormatInformes(String formatInformes) {
        this.formatInformes = formatInformes;
    }

    public ArrayList<BeanEntrevista> getListEntrevistas() {
        return listEntrevistas;
    }

    public void setListEntrevistas(ArrayList<BeanEntrevista> listEntrevistas) {
        this.listEntrevistas = listEntrevistas;
    }

    public BeanEntrevista getSelectedEntrevista() {
        return selectedEntrevista;
    }

    public void setSelectedEntrevista(BeanEntrevista selectedEntrevista) {
        this.selectedEntrevista = selectedEntrevista;
    }

    public ArrayList<BeanIncidenciasDia> getIncidencies() {
        return incidencies;
    }

    public void setIncidencies(ArrayList<BeanIncidenciasDia> incidencies) {
        this.incidencies = incidencies;
    }

    public java.util.Date getDia() {
        return dia;
    }

    public void setDia(java.util.Date dia) {
        this.dia = dia;
    }

    public BeanIncidenciasDia getIncidenciaSelected() {
        return incidenciaSelected;
    }

    public void setIncidenciaSelected(BeanIncidenciasDia incidenciaSelected) {
        this.incidenciaSelected = incidenciaSelected;
    }

    public String getSpecialDates() {
        return specialDates;
    }

    public boolean isFestiu() {
        return festiu;
    }

    public BeanNovaEntrevista getBeanEntrevista() {
        return beanEntrevista;
    }

    public void setBeanEntrevista(BeanNovaEntrevista beanEntrevista) {
        this.beanEntrevista = beanEntrevista;
    }
        
}
