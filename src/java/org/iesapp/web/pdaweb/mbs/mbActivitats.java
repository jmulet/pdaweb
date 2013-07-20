/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

/**
 *
 * @author Josep
 */ 
  
import org.iesapp.core.util.Client;
import org.iesapp.core.util.CoreCfg;
import java.util.*;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import org.apache.poi.hssf.record.CFRuleRecord.ComparisonOperator;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.iesapp.clients.sgd7.actividades.ActividadClase;
import org.iesapp.clients.sgd7.actividades.BeanActividadClase;
import org.iesapp.clients.sgd7.actividades.BeanActividadesAlumno;
import org.iesapp.clients.sgd7.actividades.BeanConceptos;
import org.iesapp.clients.sgd7.actividades.CellModel;
import org.iesapp.clients.sgd7.actividades.ColumnModel;
import org.iesapp.clients.sgd7.actividades.Conceptos;
import org.iesapp.clients.sgd7.actividades.DynamicTable;
import org.iesapp.clients.sgd7.clases.BeanClase;
import org.iesapp.clients.sgd7.evaluaciones.BeanEvaluaciones;
import org.iesapp.clients.sgd7.evaluaciones.EvaluacionesCollection;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.util.DataCtrl;
import org.iesapp.util.StringUtils;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.context.RequestContext;
import org.primefaces.event.DragDropEvent;
import org.iesapp.web.cloudws.FacesUtil;

@Named(value = "mbActivitats")
//@ManagedBean
@SessionScoped
//@ViewScoped
public class mbActivitats implements java.io.Serializable {
 
    private BeanProfesor selectedUser;
    private BeanActividadClase activitySelected;
    private ArrayList<BeanActividadClase> listact;
    private SelectItem[] clasesProfe;
    private String selectedClase="";
    private ArrayList<BeanEvaluaciones> evaluaciones;
    private BeanActividadClase novaActivitat;
    private SelectItem[] avaluacions;
    private String selectedAvaluacio="";
    private ArrayList<BeanActividadesAlumno> actividadesAlumno;
    private BeanActividadesAlumno activityAlumnoSelected;
    private boolean selectedall = false;
    private final Date avui;
    private ArrayList<BeanConceptos> listConceptes;
    private BeanConceptos selectedConcepte;
    private String chooseConcepte="";
    private int maxAssignable;
    private boolean notaRapida = false;
    private int renderedPage=1;
    private List<ColumnModel> columnesLabel;    
    private List<List<CellModel>> columnesNotes;  
    private String headerTaula;
    private final ArrayList<BeanClase> horari;
    private int idevalActual;
    private ActividadClase activityBckup;
    private final Client client;
    
    public mbActivitats() {
          
        client = (Client) FacesUtil.getSessionMapValue("client");
        selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser");
        novaActivitat =  new BeanActividadClase();
        novaActivitat.setFecha(new java.util.Date());
        novaActivitat.setPublicarWEB(true);
        novaActivitat.setConcepto(new BeanConceptos());
                
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);
        avui = cal.getTime();
        
        
        activitySelected = new BeanActividadClase();
        selectedConcepte = new BeanConceptos();
       
        listact = new ArrayList<BeanActividadClase>();
        //Evaluacions
        evaluaciones = client.getSgdClient().getEvaluacionesCollection().getEvaluaciones();
        
        int nn2 = evaluaciones.size();
        avaluacions = new SelectItem[nn2];
        
        //Important cal saber amb la data de quina avaluacio es tracta
        //Això només caldria fer-ho una vegada al principi
        idevalActual = EvaluacionesCollection.getEvaluacion(client.getSgdClient().getSgd(), avui);
               
        for(int i=0; i<nn2; i++)
        {
            avaluacions[i] = new SelectItem(evaluaciones.get(i).getId()+"", evaluaciones.get(i).getValorExportable());
            if(evaluaciones.get(i).getId()==idevalActual)
            {
                selectedAvaluacio = idevalActual+"";
            }
        }
        if(idevalActual==0) 
        {
            idevalActual = evaluaciones.get(0).getId(); //Agafaria la 1a avaluacio per defecte
        }
        //Carrega les clases que té el professor (exclou la tutoria)
        
        horari = client.getSgdClient().getClasesCollection().getClasesProfe(selectedUser.getIdProfesor());
        
        int nn = horari.size();
        clasesProfe = new SelectItem[nn];
        
        ////System.out.println("clases profe sise::"+nn);
        selectedClase = null;
        BeanClase sessionMapValue = (BeanClase) FacesUtil.getSessionMapValue("selectedBeanClase");
        
        
        for(int i=0; i<nn; i++)
        {
            ////System.out.println(horari.get(i).getMateria());
            clasesProfe[i] = new SelectItem(horari.get(i).getIdClase()+"", 
                    horari.get(i).getMateria()+"  "+horari.get(i).getGrupo());
            if(sessionMapValue!=null && sessionMapValue.getIdClase()==horari.get(i).getIdClase()) {
                selectedClase = horari.get(i).getIdClase()+"";
            }
        }
        if(nn>0 && selectedClase==null)
        {
            selectedClase = horari.get(0).getIdClase()+"";
        }
        if(selectedClase!=null) {
            loadActivitats();
        }
        
        
        //Prova per generar llistats de notes
        
        columnesLabel = new ArrayList<ColumnModel>();
        columnesNotes = new ArrayList<List<CellModel>>();
    }

  public void onDrop(DragDropEvent ddEvent)
  {
      
        String draggedId = ddEvent.getDragId(); //Client id of dragged component
        String droppedId = ddEvent.getDropId(); //Client id of dropped component
        String realdraggedId = StringUtils.AfterFirst(draggedId, "tableconcept:");
        realdraggedId = StringUtils.BeforeFirst(realdraggedId, ":");
        
        String realdroppedId = StringUtils.AfterFirst(droppedId, "listact:");
        realdroppedId = StringUtils.BeforeFirst(realdroppedId, ":");
        
        int conceptPos = (int) Double.parseDouble(realdraggedId);
        int actPos = (int) Double.parseDouble(realdroppedId);
        
          //System.out.println("he fet drop desde "+draggedId+" fins "+droppedId);
        //System.out.println("he fet drop desde "+realdraggedId+" fins "+realdroppedId);
 
        BeanConceptos concepto = listConceptes.get(conceptPos);
        //System.out.println("He triat el concepte ->"+ concepto);
        //System.out.println("He triat l'activitat ->"+ listact.get(actPos).getMapGrupAct());
        
        //Fa un update
        listact.get(actPos).setConcepto(concepto);
        ActividadClase acc = client.getSgdClient().getActividadClase(listact.get(actPos));
        acc.saveConceptesOnly();        
  }
  
    
//    
//    public void init() throws JRException{  
//        JRBeanCollectionDataSource beanCollectionDataSource=new JRBeanCollectionDataSource(listOfUser);  
//        jasperPrint=JasperFillManager.fillReport("C:\\Users\\ramki\\Desktop\\report.jasper", new HashMap(),beanCollectionDataSource);  
//    }  
//     
//   public void exportPDF(ActionEvent actionEvent) throws JRException, IOException{  
//       init();  
//       HttpServletResponse httpServletResponse=(HttpServletResponse)FacesContext.getCurrentInstance().getExternalContext().getResponse();  
//      httpServletResponse.addHeader("Content-disposition", "attachment; filename=report.pdf");  
//       ServletOutputStream servletOutputStream=httpServletResponse.getOutputStream();  
//       JasperExportManager.exportReportToPdfStream(jasperPrint, servletOutputStream);  
//   } 
    
    public void onselectedall()
    {
        ////System.out.println("onselectall is called "+selectedall+ "the activity selected is "+activitySelected);
        if(activitySelected==null) {
            return;
        }
        for(int i=0; i<activitySelected.getAssignacions().size(); i++)
        {
            activitySelected.getAssignacions().get(i).setSelected(selectedall);
        }
    }
    
    /**
     * Prepara una nova activitat sabent el concepte triat
     * @param idConcepteTriat 
     *
     **/
    public void cleanNova(int idConcepteTriat)
    {
        //System.out.println("he entrat a cleanNovva "+idConcepteTriat);
        int ordre = -1;
        for(int i=0; i<horari.size(); i++)
        {
            if(selectedClase.equals(""+horari.get(i).getIdClase()))
            {
                ordre = i;
                break;
            }
        }
        
        if(ordre<0) {
            return;
        } //No ha trobat el grup
        
        ActividadClase acc = client.getSgdClient().getActividadClase(selectedUser.getIdProfesor(), horari.get(ordre)); 
        
        //Li assigna el concepte triat per l'activitat
//        //System.out.println("Estic cercant el concepte amb id "+idConcepteTriat);
        
        for(int i=0; i<listConceptes.size(); i++)
        {
//            //System.out.println("No aquest: "+new Conceptos(listConceptes.get(i)).toString());
            if(listConceptes.get(i).getId()==idConcepteTriat)
            {                
                Conceptos concept = client.getSgdClient().getConceptos(listConceptes.get(i));
                acc.setConcepto(concept.getBean());
//                //System.out.println("!!!!!!!!!!!!!!!!!!  He trobat el concepte....");
//                //System.out.println(concept.toString());
                break;
            }
        }
        
        //Aplica els valors per defecte del concepte
        acc.setDescripcion(acc.getConcepto().getTextoActividad());
        acc.setEvaluable(acc.getConcepto().isEvaluable());
        acc.setPublicarWEB(acc.getConcepto().isWeb());
        
        //Aquests parametres haurien d'esser default
        acc.setFecha(new java.sql.Date(avui.getTime()));
        acc.setIdProfesores(selectedUser.getIdProfesor());
        acc.setIdEvaluacionesDetalle(idevalActual);
        acc.loadAssignacions(true); //podriem posar crea assignacions en blank
                                //Les assignacions no tenen id d'activitat ja encara no ha estat guardada
        ////System.out.println("S'ha preparat aquesta activitat..."+acc.getMapGrupAct().toString()+acc.toString());
        
        
        novaActivitat = acc.getBean();
       
        
        ////System.out.println("aquest es el definitiu "+novaActivitat.isEvaluable()+"; "+novaActivitat.isPublicarWEB()+"; "+ novaActivitat.getConcepto().getNombre());
    }
    
    
    public void onUpdateActivitat() {
        
        ////System.out.println("\t Estic en onUpdateActivitat activityselected= "+activitySelected);
        if(activitySelected == null) {
            return;
        }
        client.getIesClient().getCoreCfg().addAction("update-activ");
               
        //Important cal saber amb la data de quina avaluacio es tracta
//        int ideval = EvaluacionesCollection.getEvaluacion(mysql, activitySelected.getFecha());
//        ////System.out.println("\t des de mysql "+ideval);
//        if(ideval==0) 
//        {
//            ideval = evaluaciones.get(0).getId();
//            ////System.out.println("\t arregant.lo "+ideval);
//        }
//        
        //comprova si hi ha alguna activitat amb el mateix nom el mateix dia
        int nup = 0; 
        if(client.getSgdClient().getActividadesCollection().checkValidityActivity(activitySelected))
        {
             nup = client.getSgdClient().getActividadClase(activitySelected).save();
        }
        else
        {
             FacesContext context = FacesContext.getCurrentInstance();
             context.addMessage(null, new FacesMessage( FacesMessage.SEVERITY_WARN, "Error:",
                     "Aquest dia ja hi ha una activitat amb la mateixa descripció."));
        }
        
    
        //Per evitar tornar a carregar totes les activitats estic fent un update nomes
        //de l'activitat que s'ha modificat
        if(nup>0)
        {
            //cerca quin és el concepte que s'ha triat
            for(int i=0; i<listConceptes.size();i++)
            {
                if(listConceptes.get(i).getId()==activitySelected.getConcepto().getId())
                {
                    Conceptos tmp1 = client.getSgdClient().getConceptos(listConceptes.get(i));
                    activitySelected.setConcepto( tmp1.getBean() );
                    ////System.out.println("s'ha assignat un concepte");
                    break;
                }
            }
            
            //fa un update de l'activitat
            for(int i=0; i<listact.size(); i++)
            {
                if(listact.get(i).getOrdre()==activitySelected.getOrdre())
                {
                    ActividadClase tmp2 = client.getSgdClient().getActividadClase(activitySelected);
                    listact.set(i, tmp2.getBean());
                    ////System.out.println("s'ha actualitat listact");
                    break;
                }
                        
            }
            
            RequestContext.getCurrentInstance().execute("edita.hide()");
        }
    }
    
    public void onSaveActivitat() {
        ////System.out.println("onSaveActivitat ...");
        
        if(novaActivitat == null) {
            return;
        }
        
       
        //Important cal saber amb la data de quina avaluacio es tracta
//        int ideval = EvaluacionesCollection.getEvaluacion(client.getSgdClient().getSgd(), novaActivitat.getFecha());
//        ////System.out.println("\t des de mysql "+ideval);
//        if(ideval==0) 
//        {
//            ideval = evaluaciones.get(0).getId();
//            ////System.out.println("\t arregant.lo "+ideval);
//        }
//        
        
        ActividadClase aac = client.getSgdClient().getActividadClase(novaActivitat);
        FacesContext context = FacesContext.getCurrentInstance();
        
         if(client.getSgdClient().getActividadesCollection().checkValidityActivity(novaActivitat))
         {
             aac.save();
             ////System.out.println("aac.assignacions is"+aac.getAssignacions().size());
             aac.saveAssignacions();
            ////System.out.println("actividad guardada::: "+aac.getMapGrupAct().toString() + aac.toString());

             if (!aac.getMapGrupAct().values().contains(-1)) {
                context.addMessage(null, new FacesMessage("Informació", "S'ha introduït l'activitat " + novaActivitat.getMapGrupAct().values().toString()));
            } else {
                context.addMessage(null, new FacesMessage("Error", "Impossible introduir l'activitat"));
            }
        
            RequestContext.getCurrentInstance().execute("nova.hide()");
            loadActivitats();
        }
        else
        {
             context.addMessage(null, new FacesMessage( FacesMessage.SEVERITY_ERROR, "Error:",
                     "Avui ja hi ha una activitat amb aquesta descripció."));
        }
     
    }

    public void deleteActivity()
    {
        ////System.out.println("deleteActivity");
//        he d'agafar la selectedInc
         FacesContext context = FacesContext.getCurrentInstance();
        
        if (activitySelected == null) {
            context.addMessage(null, new FacesMessage("Error", "Impossible esborrar l'activitat "));
            return;
        }
        client.getIesClient().getCoreCfg().addAction("del-activ");
        
        int removeInc = client.getSgdClient().getActividadClase(activitySelected).deleteAll();
         
        ////System.out.println("deleteActivity -> nuup="+removeInc);
       
        if (removeInc > 0) {
            context.addMessage(null, new FacesMessage("Informació", "S'ha esborrat l'activitat " ));
        } else {
            context.addMessage(null, new FacesMessage("Error", "Impossible esborrar l'activitat "));
        }
        loadActivitats();
     
    }
    
    public void onRowEditor(org.primefaces.event.RowEditEvent event)
    {
////        BeanActividades editedAct = (BeanActividades) event.getObject();
////        ////System.out.println("Faig update de l'activitat "+editedAct.getMapIds().toString());
////        int nup = Activities.updateActividad(client.getSgdClient().getSgd(), editedAct);
////        if(nup>0)
////        {
////            FacesContext context = FacesContext.getCurrentInstance();
////            context.addMessage(null, new FacesMessage("Informació","S'ha desat l'activitat correctament."));
////        }
       
    }
    
    public void loadActivitats()  
    {
        //renderedPage = 1;
        
        listact = new ArrayList<BeanActividadClase>();
        if(selectedClase==null || selectedClase.isEmpty())
        {
            selectedClase="";
            return;  
        } 
        if(selectedAvaluacio==null) {
            selectedAvaluacio="0";
        }
        
       
       
        int ordre = -1;
        for(int i=0; i<horari.size(); i++)
        {
            if(selectedClase.equals(""+horari.get(i).getIdClase()))
            {
                ordre = i;
                break;
            }
        }
        
        if(ordre<0) {
            return;
        }
              
        int ideval = -1;
        if(selectedAvaluacio.isEmpty()) {
            selectedAvaluacio="0";
        }
        try
        {
            ideval = (int) Double.parseDouble(selectedAvaluacio);
        }
        finally
        {
            
        }
        
        ////System.out.println("estic en loadActivitats idclase="+idclase+"; ideval="+ideval);
        
        if(ideval<0) {
            return;
        }
        
        
       //Tambe ha de carregar la llista de conceptes
       generateConceptosClase(horari.get(ordre).getIdClase());

       listact = client.getSgdClient().getActividadesCollection().loadActividades(selectedUser.getIdProfesor(), horari.get(ordre), ideval, 0, "DESC",0);
        ////System.out.println("s'han trobat aquestes activitats "+listact.size());
        
        if(!listact.isEmpty())
        {
            activitySelected = listact.get(0);
        }
       
        //Assegura que mostra la 1a pàgina (aixo per arreglar el que sembla un bug)
        DataTable dataTable = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent("form:listact"); 
        if(dataTable!=null) {
            dataTable.setFirst(0);
        }
        
    }
    
    
    public void setFechaEntrega(int ordre)
    {
       ////System.out.println("setting fecha de entrega...");
       activitySelected.getAssignacions().get(ordre).setFechaEntrega(new java.util.Date());
    }
    
    public void onActivitySelected(org.primefaces.event.SelectEvent ev)
    {
      activitySelected = (BeanActividadClase) ev.getObject();         
    }
    
    /**
     * Listening to click boto assigna activitat
     */
    public void assignaActivitat()
    {        
        selectedall = false;
        client.getIesClient().getCoreCfg().addAction("asign-activ");
        
        ////System.out.println("estic en assignacions...");
        if(activitySelected==null) {
            return;
        }
        ////System.out.println("l'activitat seleccionada es "+activitySelected.getDescripcion());
        int idClase = -1;
        
        if(selectedAvaluacio.isEmpty()) {
            selectedAvaluacio="0";
        }
        try
        {
            idClase = (int) Double.parseDouble(selectedClase);
        }
        finally
        {
            
        }
        ////System.out.println("la id de la clase es..."+idClase);
        
        HashMap<Integer, Integer> mapIds = activitySelected.getMapGrupAct();
        ////System.out.println("map ids are "+mapIds.toString());
        ActividadClase acc = client.getSgdClient().getActividadClase(activitySelected);
        acc.loadAssignacions(false);
        activitySelected = acc.getBean();
        
        //Cream un backup de l'activitat
        activityBckup = client.getSgdClient().getActividadClase(activitySelected);
        
        ////System.out.println("s'han aplicat les assignacions, size is"+activitySelected.getAssignacions().size());
        
//        int idactivitat = activitySelected.getId();
//        int idgrupasig = activitySelected.getIdGrupAsig().get(0);
//        
//        actividadesAlumno = Data.getActividadesAlumno(client.getSgdClient().getSgd(), idgrupasig, idactivitat);
//        ////System.out.println("S'han trobat les assignacions de id="+idactivitat+"; idgrupasig="+idgrupasig+"; n.="+actividadesAlumno.size());
//        for(int i=0; i<actividadesAlumno.size(); i++)
//        {
//            ////System.out.println(actividadesAlumno.get(i).isSelected()+"; "+actividadesAlumno.get(i).getNombre()+"; "+
//                    actividadesAlumno.get(i).getFechaEntrega()+";"+actividadesAlumno.get(i).getNota());
//        }
    }
 
    
    /**
     * Listening to Accept changes in assignacions
     * 30-10-12: Cal evitar que quan s'editen les assignacions es tornin a 
     * generar totes amb una nova id, cal mantenir la mateixa id, esborrar les que
     * deixen d'estar assignats, update les existens i insert les noves
     */
    public void doAssignacions()    
    {
       if(!activitySelected.getAssignacions().isEmpty())
        {
            ActividadClase acc = client.getSgdClient().getActividadClase(activitySelected);
            acc.saveAssignacions(activityBckup); //desa les assignacions que siguin diferents o que hagin canviat
            loadActivitats();
        }
        
    }

    
//    public void editaConcepte(org.primefaces.event.SelectEvent ev)
//    {
//        selectedConcepte = (BeanConceptos) ev.getObject();
//    }
//   
     public void esborraConcepte()
     {
         client.getIesClient().getCoreCfg().addAction("delConcept-activ");
        
         if(selectedConcepte==null) {
            return;
        }
         int nup = client.getSgdClient().getConceptos(selectedConcepte).delete();
         
         //torna a carregar els conceptes disponibles i a determinar el maxim assignable
          int idclase = (int) Double.parseDouble(selectedClase);
          generateConceptosClase(idclase);
          
         //Actualitza les activitats (les que tenien associat aquest concepte passen a global)
          for(int i=0; i<listact.size(); i++)
          {
              if(listact.get(i).getConcepto().getId()==selectedConcepte.getId())
              {
                  listact.get(i).setConcepto(new BeanConceptos());
              }
          }
     }
     
     public void preparaNouConcepte()
     {
         client.getIesClient().getCoreCfg().addAction("newConcept-activ");
        
         selectedConcepte = new BeanConceptos();
         selectedConcepte.setId(0);
         selectedConcepte.setHtmlColor(randomHexColor());
         selectedConcepte.setNombre("");
         selectedConcepte.setIdProfesores(selectedUser.getIdProfesor());
         int idclase = (int) Double.parseDouble(selectedClase);
         selectedConcepte.setIdClase(idclase);
         ////System.out.println("prepara nou concepte"+selectedConcepte.getId());
     }
     
   
     public void desaConcepte()
     {
         if(Conceptos.checkValidity(selectedConcepte, client.getSgdClient()))
         {
             Conceptos concept = client.getSgdClient().getConceptos(selectedConcepte);
             int nup = concept.save();
             selectedConcepte.setId(concept.getId());

             //torna a carregar els conceptes disponibles i a determinar el maxim assignable
              int idclase = (int) Double.parseDouble(selectedClase);
              generateConceptosClase(idclase);
              
              //Actualitza les activitats
              for(int i=0; i<listact.size(); i++)
              {
                  if(listact.get(i).getConcepto().getId()==selectedConcepte.getId())
                  {
                      listact.get(i).setConcepto( client.getSgdClient().getConceptos(selectedConcepte).getBean() );
                  }
              }
              
              RequestContext.getCurrentInstance().execute("editaConcepte.hide()");
         }
         else
         {
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage("Error", "El nom del concepte és inadequat"));        
         }
     }
 
     public void cleanLlistatActivitats()
     {
        columnesLabel.clear();
        columnesNotes.clear();
        chooseConcepte="";
     }
     
     public void loadLlistatActivitats()
     {
        client.getIesClient().getCoreCfg().addAction("meanConcept-activ");
        
        int idConcepte=0;
        try
        {
         idConcepte= Integer.parseInt(chooseConcepte);
        }
        catch(Exception e)
        {            
            //dismiss
        }
        
        
        int ordre = -1;
        for(int i=0; i<horari.size(); i++)
        {
            if(selectedClase.equals(""+horari.get(i).getIdClase()))
            {
                ordre = i;
                break;
            }
        }
        
        if(ordre < 0 || idConcepte==0) 
        {
            columnesLabel.clear();
            columnesNotes.clear();
            return;
        }
        
        //Important cal saber amb la data de quina avaluacio es tracta
        //int ideval = EvaluacionesCollection.getEvaluacion(mysql, new java.util.Date());
        int ideval= (int) Double.parseDouble(selectedAvaluacio);
        if(ideval<=0) {
            ideval = EvaluacionesCollection.getEvaluacion(client.getSgdClient().getSgd(), new java.util.Date());
        }
        
        DynamicTable dt = client.getSgdClient().getActividadesCollection().getInformeDetallatActivitats(horari.get(ordre), selectedUser.getIdProfesor(), ideval, idConcepte);
        columnesLabel = dt.getColumnLabels();
        columnesNotes= dt.getColumnValues();
       
        
        headerTaula="";
        
        for(int i=0; i<clasesProfe.length; i++)
        {
            if(clasesProfe[i].getValue().equals(selectedClase))
            {
                headerTaula += clasesProfe[i].getLabel();
                break;
            }
            
        }
        
        for(int i=0; i<avaluacions.length; i++)
        {
            if(avaluacions[i].getValue().equals(selectedAvaluacio))
            {
                headerTaula += " ("+avaluacions[i].getLabel()+")";
                break;
            }   
        }
                
     }
     
     
     
     // Obté la nota mitjana d'avaluació
     
     public void cleanResumActivitats()
     {
        client.getIesClient().getCoreCfg().addAction("meanAvaluacio-activ");
        int ordre = -1;
        for(int i=0; i<horari.size(); i++)
        {
            if(selectedClase.equals(""+horari.get(i).getIdClase()))
            {
                ordre = i;
                break;
            }
        }
        
        
        if(ordre<0) 
        {
            columnesLabel.clear();
            columnesNotes.clear();
            return;
        }
        
        //Important cal saber amb la data de quina avaluacio es tracta
        //int ideval = EvaluacionesCollection.getEvaluacion(mysql, new java.util.Date());
        int ideval= (int) Double.parseDouble(selectedAvaluacio);
        if(ideval<=0) {
             ideval = EvaluacionesCollection.getEvaluacion(client.getSgdClient().getSgd(), new java.util.Date());
         }
        
        DynamicTable dt = client.getSgdClient().getActividadesCollection().getInformeResumitActivitats(horari.get(ordre), selectedUser.getIdProfesor(), ideval);
        columnesLabel = dt.getColumnLabels();
        columnesNotes= dt.getColumnValues();
       
        
        headerTaula="";
        
        for(int i=0; i<clasesProfe.length; i++)
        {
            if(clasesProfe[i].getValue().equals(selectedClase))
            {
                headerTaula += clasesProfe[i].getLabel();
                break;
            }
            
        }
        
        for(int i=0; i<avaluacions.length; i++)
        {
            if(avaluacions[i].getValue().equals(selectedAvaluacio))
            {
                headerTaula += " ("+avaluacions[i].getLabel()+")";
                break;
            }   
        }
     }
     
    public void updateEvaluable(int pos)
    {
        BeanActividadClase get = listact.get(pos);
         //Una actividad que no era avaluable i ara si que ho es
        //per defecte se li assigna un pes de 100
        if(get.isEvaluable())
        {
            get.setPeso(100);
        }
        
        ActividadClase acc = client.getSgdClient().getActividadClase(get);
        acc.save();
        //Actualiza el bean en la llista
        listact.set(pos, acc.getBean());
       
    }
        
    public void updatePeso(int pos)
    {
        BeanActividadClase get = listact.get(pos);
        ActividadClase acc = client.getSgdClient().getActividadClase(get);
        acc.savePesosOnly();
        //Actualiza el bean en la llista
        listact.set(pos, acc.getBean());
    }
    
    public String date2str(java.util.Date date)
    {
        String str = date==null? "":new DataCtrl(date).getDiaMesComplet();
        return str;
    }
    
    private String randomHexColor()
    {
        int integer=(int) Math.floor(Math.random()*16777215);
        return Integer.toHexString(integer);
    }
    
    /**
     * @return the selectedUser
     */
    public BeanProfesor getSelectedUser() {
        return selectedUser;
    }

    /**
     * @param selectedUser the selectedUser to set
     */
    public void setSelectedUser(BeanProfesor selectedUser) {
        this.selectedUser = selectedUser;
    }

    /**
     * @return the activitySelected
     */
    public BeanActividadClase getActivitySelected() {
        return activitySelected;
    }

    
    
    /**
     * @param activitySelected the activitySelected to set
     */
    public void setActivitySelected(BeanActividadClase activitySelected) {
        this.activitySelected = activitySelected;
    }

    /**
     * @return the listact
     */
    public ArrayList<BeanActividadClase> getListact() {
        return listact;
    }

    /**
     * @param listact the listact to set
     */
    public void setListact(ArrayList<BeanActividadClase> listact) {
        this.listact = listact;
    }

    /**
     * @return the clasesProfe
     */
    public SelectItem[] getClasesProfe() {
        return clasesProfe;
    }

    /**
     * @param clasesProfe the clasesProfe to set
     */
    public void setClasesProfe(SelectItem[] clasesProfe) {
        this.clasesProfe = clasesProfe;
    }

    /**
     * @return the selectedClase
     */
    public String getSelectedClase() {
        return selectedClase;
    }

    /**
     * @param selectedClase the selectedClase to set
     */
    public void setSelectedClase(String selectedClase) {
        this.selectedClase = selectedClase;
    }

    /**
     * @return the novaActivitat
     */
    public BeanActividadClase getNovaActivitat() {
        return novaActivitat;
    }

    /**
     * @param novaActivitat the novaActivitat to set
     */
    public void setNovaActivitat(BeanActividadClase novaActivitat) {
        this.novaActivitat = novaActivitat;
    }

    /**
     * @return the avaluacions
     */
    public SelectItem[] getAvaluacions() {
        return avaluacions;
    }

    /**
     * @param avaluacions the avaluacions to set
     */
    public void setAvaluacions(SelectItem[] avaluacions) {
        this.avaluacions = avaluacions;
    }

    /**
     * @return the selectedAvaluacio
     */
    public String getSelectedAvaluacio() {
        return selectedAvaluacio;
    }

    /**
     * @param selectedAvaluacio the selectedAvaluacio to set
     */
    public void setSelectedAvaluacio(String selectedAvaluacio) {
        this.selectedAvaluacio = selectedAvaluacio;
    }

    /**
     * @return the actividadesAlumno
     */
    public ArrayList<BeanActividadesAlumno> getActividadesAlumno() {
        return actividadesAlumno;
    }

    /**
     * @param actividadesAlumno the actividadesAlumno to set
     */
    public void setActividadesAlumno(ArrayList<BeanActividadesAlumno> actividadesAlumno) {
        this.actividadesAlumno = actividadesAlumno;
    }

    /**
     * @return the activityAlumnoSelected
     */
    public BeanActividadesAlumno getActivityAlumnoSelected() {
        return activityAlumnoSelected;
    }

    /**
     * @param activityAlumnoSelected the activityAlumnoSelected to set
     */
    public void setActivityAlumnoSelected(BeanActividadesAlumno activityAlumnoSelected) {
        this.activityAlumnoSelected = activityAlumnoSelected;
    }

    /**
     * @return the selectedall
     */
    public boolean isSelectedall() {
        return selectedall;
    }

    /**
     * @param selectedall the selectedall to set
     */
    public void setSelectedall(boolean selectedall) {
        this.selectedall = selectedall;
    }

    /**
     * @return the listConceptes
     */
    public ArrayList<BeanConceptos> getListConceptes() {
        return listConceptes;
    }

    /**
     * @return the selectedConcepte
     */
    public BeanConceptos getSelectedConcepte() {
        return selectedConcepte;
    }

    /**
     * @param selectedConcepte the selectedConcepte to set
     */
    public void setSelectedConcepte(BeanConceptos selectedConcepte) {
        this.selectedConcepte = selectedConcepte;
    }

 
    /**
     * @return the maxAssignable
     */
    public int getMaxAssignable() {
        return maxAssignable;
    }
 
    /**
     * @return the notaRapida
     */
    public boolean isNotaRapida() {
        return notaRapida;
    }

    /**
     * @param notaRapida the notaRapida to set
     */
    public void setNotaRapida(boolean notaRapida) {
        this.notaRapida = notaRapida;
    }

    /**
     * @return the columnesLabel
     */
    public List<ColumnModel> getColumnesLabel() {
        return columnesLabel;
    }

    /**
     * @param columnesLabel the columnesLabel to set
     */
    public void setColumnesLabel(List<ColumnModel> columnesLabel) {
        this.columnesLabel = columnesLabel;
    }

    /**
     * @return the columnesNotes
     */
    public List<List<CellModel>> getColumnesNotes() {
        return columnesNotes;
    }

    /**
     * @return the renderedPage
     */
    public int getRenderedPage() {
        return renderedPage;
    }

    /**
     * @param renderedPage the renderedPage to set
     */
    public void setRenderedPage(int renderedPage) {
        this.renderedPage = renderedPage;
    }

    /**
     * @return the chooseConcepte
     */
    public String getChooseConcepte() {
        return chooseConcepte;
    }

    /**
     * @param chooseConcepte the chooseConcepte to set
     */
    public void setChooseConcepte(String chooseConcepte) {
        this.chooseConcepte = chooseConcepte;
    }

    /**
     * @return the headerTaula
     */
    public String getHeaderTaula() {
        return headerTaula;
    }

    private void generateConceptosClase(int idClase) {
        listConceptes = client.getSgdClient().getActividadesCollection().getConceptosClase(selectedUser.getIdProfesor(), idClase);
        maxAssignable = listConceptes.size() < 3 ? 100 : listConceptes.get(0).getPorcentaje();
    }

 
     public void postProcessXLS2(Object document) {
         XLSProcessing(document, 2);
     }
    
     public void postProcessXLS(Object document) {
         XLSProcessing(document, 1);
     }
     
    private void XLSProcessing(Object document, int mode) {
        final int iniRow = 0;
        client.getIesClient().getCoreCfg().addAction("xls-activ");
        
        String concept = "";
        java.awt.Color conceptColor = null;
        if(mode==1)
        {
            int idConcepte=0;
            try
            {
                idConcepte= (int) Double.parseDouble(chooseConcepte);
            }
            finally
            {            
            }
            for(int i=0; i<listConceptes.size();i++)
            {
                if( chooseConcepte.equals( listConceptes.get(i).getId()+""))
                {
                    concept = " · Concepte: "+listConceptes.get(i).getNombre();
                    int intg = Integer.parseInt(listConceptes.get(i).getHtmlColor(),16); 
                    conceptColor = new java.awt.Color(intg);
                    break;
                }
            }
            
        }
    
        HSSFWorkbook wb = (HSSFWorkbook) document;
        wb.removeSheetAt(0);
        HSSFSheet sheet = wb.createSheet("Full 1");     
        HSSFRow createRow = sheet.createRow(iniRow);
        
        //N. de decimals
        HSSFDataFormat df = wb.createDataFormat(); 
        short dataFormat = df.getFormat("0.00##"); //can also use a format like "0.00##" if you don't want extra decimal places to display when they don't exist 
   
        //Define Styles
        HSSFCellStyle styleTitle = wb.createCellStyle();
        if(mode==1)
        {
            HSSFPalette palette = wb.getCustomPalette();
            palette.setColorAtIndex(HSSFColor.LAVENDER.index, (byte)conceptColor.getRed(), (byte)conceptColor.getGreen(), (byte)conceptColor.getBlue());
            styleTitle.setFillForegroundColor(HSSFColor.LAVENDER.index);
        }
        else
        {
            styleTitle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        }
        
        styleTitle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        styleTitle.setBorderBottom(CellStyle.BORDER_DOUBLE);
        styleTitle.setAlignment(CellStyle.ALIGN_LEFT);
        HSSFFont font = wb.createFont();
        font.setFontHeight(HSSFFont.BOLDWEIGHT_BOLD);
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        styleTitle.setFont(font);        
        
        //Summary Style
        HSSFCellStyle styleSummary = wb.createCellStyle();
        font = wb.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
        styleSummary.setFont(font);        
        
        //Summary Style-Numeric
         HSSFCellStyle styleSummaryNumeric = wb.createCellStyle();
         styleSummaryNumeric.cloneStyleFrom(styleSummary);
         styleSummaryNumeric.setDataFormat(dataFormat);
         
         //Style header
        HSSFCellStyle styleHeader = wb.createCellStyle();
        styleHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        styleHeader.setFillPattern(CellStyle.SOLID_FOREGROUND);
        styleHeader.setAlignment(CellStyle.ALIGN_CENTER);
        styleHeader.setBorderBottom(CellStyle.BORDER_THIN);
        styleHeader.setBorderLeft(CellStyle.BORDER_THIN);
        styleHeader.setWrapText(true);
        HSSFFont font2 = wb.createFont();
        font2.setFontName("Arial");
        font2.setFontHeightInPoints((short) 11);
        font2.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);      
        styleHeader.setFont(font2);        
          
         //credits header
        HSSFCellStyle creditsStyle = wb.createCellStyle();
        creditsStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        creditsStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        creditsStyle.setAlignment(CellStyle.ALIGN_LEFT);
        creditsStyle.setBorderBottom(CellStyle.BORDER_THIN);
        creditsStyle.setBorderLeft(CellStyle.BORDER_THIN);
        creditsStyle.setWrapText(true);
        HSSFFont font3 = wb.createFont();
        font3.setFontName("Arial");
        font3.setColor(IndexedColors.WHITE.getIndex());
        font3.setFontHeightInPoints((short) 8);  
        creditsStyle.setFont(font3);        
        
        
        //Estil numeric aprovat
         HSSFCellStyle passStyle = wb.createCellStyle();
         passStyle.setDataFormat(dataFormat);
          
        //Suspès style
        HSSFCellStyle failedCell = wb.createCellStyle();
        font = wb.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        failedCell.setFont(font);   
        failedCell.setDataFormat(dataFormat);
        
        
        
        //Titol
        HSSFCell createCellTitle = createRow.createCell(0); 
      
        ////System.out.println("idConcepte"+idConcepte+" "+listConceptes.size());
        createCellTitle.setCellValue(headerTaula+concept);
        createCellTitle.setCellStyle(styleTitle);
        
        
        HSSFRow createRow1 = sheet.createRow(iniRow+1);
        HSSFRow createRow2 = sheet.createRow(iniRow+2);
        HSSFRow createRow3 = sheet.createRow(iniRow+3);
        for(int j=0; j<columnesLabel.size(); j++)
        {
            HSSFCell createCell = createRow1.createCell(j);
            createCell.setCellValue(columnesLabel.get(j).getHeader().getLine1());
            createCell.setCellStyle(styleHeader);
            
            //Aquesta és la columna dels percentatges i cal fer-li un lifting per l'excel
            String tpc = columnesLabel.get(j).getHeader().getLine2();
            tpc = tpc.replace("(", "").replace(")", "").replace("%","");
            if(!tpc.isEmpty() && j>0 && j<columnesLabel.size()-1)
            {
            createCell = createRow2.createCell(j);
            createCell.setCellValue(Double.parseDouble(tpc));
            createCell.setCellStyle(styleHeader);
            }
            else if(j==0 || j==columnesLabel.size()-1)
            {
                createCell = createRow2.createCell(j);
                createCell.setCellValue(tpc);
                createCell.setCellStyle(styleHeader);
            }
            
            createCell = createRow3.createCell(j);
            createCell.setCellValue(columnesLabel.get(j).getHeader().getLine3());
            createCell.setCellStyle(styleHeader);

        }
       
         
        for(int j=0; j<columnesNotes.size(); j++)
        {
            
        createRow = sheet.createRow(j+4+iniRow);
        for(int i=0; i<columnesNotes.get(j).size(); i++)
        {
            Object obj = columnesNotes.get(j).get(i).getValue();
            if(obj instanceof java.lang.String)
            {
                HSSFCell createCell = createRow.createCell(i);
                String txt = (String) obj;
                if(!txt.equalsIgnoreCase("NP")) {
                        createCell.setCellValue(txt);
                    }
              
                if(j>columnesNotes.size()-4) {
                        createCell.setCellStyle(styleSummary);
                    }
            }
            else
            {
                HSSFCell createCell = createRow.createCell(i);
                Float fval = (Float) obj;
                createCell.setCellValue(fval);
                createCell.setCellType(Cell.CELL_TYPE_NUMERIC);
                
                if(j>columnesNotes.size()-4) //Les 3 darreres files son el summary
                {
                    createCell.setCellStyle(styleSummaryNumeric);
                }
                else
                {
                        createCell.setCellStyle(passStyle);
                    
                }
                
            }
        }
        
        }
        
        //SobreEscriu la columna de les mitjanes amb formules
        int n = columnesLabel.size();
        int rowf = columnesNotes.size()+4+iniRow;
         
        if(n>1) //Si hi ha alguna nota
        {
        
        String c1 = new CellReference(2,1).formatAsString();
        String c2 = new CellReference(2,n-2).formatAsString();
        for(int j=0; j<columnesNotes.size()-3; j++)
        {            
            String products = "";
            for(int i=1; i<columnesNotes.get(j).size()-1; i++)
            {
                String cell1 = new CellReference(2,i).formatAsString();
                String cell2 = new CellReference(j+4+iniRow,i).formatAsString();
                products += cell1+"*"+cell2+"+";
            }
            products +="0";
                    
            sheet.getRow(j+4+iniRow).getCell(n-1).setCellFormula("("+products+")/SUM("+c1+":"+c2+")");
        }
        
       
  
    //Aplica els estils com a conditional formating
    HSSFSheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
    HSSFConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule(ComparisonOperator.BETWEEN, "0","4.99");
    HSSFFontFormatting fontFmt = rule1.createFontFormatting();
    fontFmt.setFontStyle(true, false);
    fontFmt.setFontColorIndex(IndexedColors.RED.getIndex());      
    HSSFConditionalFormattingRule [] cfRules = {rule1};

    CellRangeAddress[] regions = {
        new CellRangeAddress(4,rowf-3,1,n-1)
    };

    sheetCF.addConditionalFormatting(regions, cfRules);
    }
        
            
          
        HSSFCell createCell = sheet.createRow(rowf).createCell(0);
        createCell.setCellValue("PDAWEB"+CoreCfg.VERSION+" @ "+new java.util.Date());
        createCell.setCellStyle(creditsStyle);
      
        //MergeCells
        sheet.addMergedRegion(new CellRangeAddress(0,0,0,columnesLabel.size()-1));
        sheet.addMergedRegion(new CellRangeAddress(rowf,rowf,0,columnesLabel.size()-1));
 
        //Autoajusta
        for (int i=0; i<n-1; i++)
        {
            sheet.autoSizeColumn(i);   
        }
    
                
}
    
}