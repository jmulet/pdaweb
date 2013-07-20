/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs.guardies;

  
import org.iesapp.core.util.Client;
import org.iesapp.core.util.CoreCfg;
import org.iesapp.guardies.GuardiesUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import org.iesapp.clients.iesdigital.guardies.GuardiesBean;
import org.iesapp.clients.sgd7.base.BeanTipoIncidencias;
import org.iesapp.clients.sgd7.clases.BeanClaseGuardia;
import org.iesapp.clients.sgd7.clases.BeanHoraCentro;
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
@ManagedBean(name="mbGuardies")
@SessionScoped
public class mbGuardies implements java.io.Serializable {
    private final BeanProfesor selectedUser;
    private final Date avui;
    private java.util.Date dia;
    private final String diastr;
    private SelectItem[] opcionesHoras;
    private SelectItem[] opcionesIncidencias;
    protected String selectedHora="-1";
    protected ArrayList<BeanClaseGuardia> horario;
    private ArrayList<BeanClaseGuardia> substituciones;
    private BeanClaseGuardia selectedSubstitucion;
    private final ArrayList<BeanHoraCentro> horasClase;
    private ArrayList<GuardiesBean> onduty;
  
    private String observacions="";
    private String selectedIncidencia="-1";
    private final HashMap<Integer, BeanTipoIncidencias> mapIncidencias;
    private BeanClaseGuardia selectedClase;
    private boolean amionduty = false; //Am I on duty now?
    private String pwdSU="";
    private final Client client;
    
    public mbGuardies()
    {
        selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser");
        client = (Client) FacesUtil.getSessionMapValue("client");

        Calendar cal = Calendar.getInstance();
        cal.setTime(client.getIesClient().getMysql().getServerDate());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        avui = cal.getTime();
        dia = avui;
        diastr = new DataCtrl(dia).getDataSQL();
        //Carrega les hores del centre
        horasClase = client.getSgdClient().getHoraCentro().getHorasClase(" TIMEDIFF(fin,inicio)>'00:20:00' ");
        
        //Crea un menu
        opcionesHoras = new SelectItem[ horasClase.size()+ 1];
        opcionesHoras[0] = new SelectItem("-1", "Triau una hora");
     
        selectedHora = ""+ client.getSgdClient().getHoraCentro().getCurrentIdHoraCentro();
        
        for (int i = 1; i < horasClase.size() + 1; i++) {
            opcionesHoras[i] = new SelectItem(horasClase.get(i-1).getIdHoraClase(), horasClase.get(i-1).getDescripcion());
        }

        
         //Opciones de incidencias
        mapIncidencias = client.getSgdClient().getTipoIncidencias().getMapIncidencias("guardia");
        opcionesIncidencias = new SelectItem[ mapIncidencias.size()+ 1];
        opcionesIncidencias[0] = new SelectItem("-1", "Triau una incidència");
        int i = 1;
        for(BeanTipoIncidencias bean: mapIncidencias.values())
        {
            opcionesIncidencias[i] = new SelectItem(bean.getId(), bean.getDescripcion());
            i += 1;
        }
       
        
        fillTable();
        
    }
    
    public void goToday()
    {
        
    }
    
    public void changeDate(SelectEvent event)
    {
        dia = (java.util.Date) event.getObject();
        fillTable();
    }
    
    public String gotoSubstitucions()
    {
        return "substitucions";
    }
    
    public void fillTable()
    {
        
          
        //Carrega les substitucions
        int idDiaSetmana = new DataCtrl(dia).getIntDia();
        substituciones = client.getSgdClient().getClasesCollection().getSubstitucionesByProfeGuardia(client.getIesClient().getSgd(), idDiaSetmana, dia, selectedUser.getIdProfesor());
        if(!substituciones.isEmpty()) {
            selectedSubstitucion =substituciones.get(0);
        }
        
        if(selectedHora.equals("-1")) 
        {
            onduty = new ArrayList<GuardiesBean>();
            return;
        }
        
    
        int idHoraCentro = (int) Double.parseDouble(selectedHora);
        org.iesapp.clients.sgd7.clases.ClasesGuardia cg = client.getSgdClient().getClasesGuardia(idDiaSetmana, idHoraCentro);
        
        //Carrega l'horari
        horario = cg.getHorario();
        if(!horario.isEmpty()) {
            selectedClase = horario.get(0);
        }
      
            
        onduty = new ArrayList<GuardiesBean>();
        
        //Ara caldria aplicar-li la condicio de falta
        int idHoraCentroSGD = -1;
        for(int i=0; i<horasClase.size();i++)
        {
            if(selectedHora.equals(""+horasClase.get(i).getIdHoraClase()))
            {
                idHoraCentroSGD = horasClase.get(i).getIdHoraClase();
                break;
            }
        }
        if( client.getSgdClient().getHoraCentro().getSgd2iesdhcmap().containsKey(idHoraCentroSGD) )
        {
            int idhoraiesd = client.getSgdClient().getHoraCentro().getSgd2iesdhcmap().get(idHoraCentroSGD);
            horario = GuardiesUtils.ClasesGuardiesUpdateStatus(horario, dia, idhoraiesd, client.getIesClient());
        
            //Carrega els professors que estan de guardia
            onduty = client.getIesClient().getGuardiesClient().getGuardiesCollection().getProfesGuardia(idDiaSetmana, idhoraiesd);
            
            amionduty = false;
            for(GuardiesBean bean: onduty)
            {
                if(bean.getIdProfesor().equals(selectedUser.getIdProfesor()))
                {
                    amionduty = true;
                    break;
                }
            }
                
          //  onduty.get(1).getIdProfesor();
        }
    }
    
     public void preparaSubstitucio() 
     {
           //Check if am i on duty. if not show a message explaining that only
            //teachers on duty such hour can anotate subtitutions
            this.observacions="";
            this.selectedIncidencia="-1";
            
            if(!amionduty)
            {
                pwdSU = "";
                //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Operació no permesa","Només professors de guardia en aquesta hora poden anotar substitucions."));
                RequestContext.getCurrentInstance().execute("pwdrequired.show()");
                return;
            }
                
         
          
            
            if(selectedClase!=null)
            {
                RequestContext.getCurrentInstance().execute("incvar.show()");
            }            
            
                //System.out.println("ERROR SELECTEDCLASE IS NULL");
     }
     
     public void validaSubstitucio()
     {
         String pwd = (String) CoreCfg.configTableMap.get("pdawebSU");
         if(pwd==null) {
             pwd = (String) CoreCfg.configTableMap.get("adminPwd");
         }
         
         if(pwdSU.equals(pwd))
         {
              
            if(selectedClase!=null)
            {
                RequestContext.getCurrentInstance().execute("incvar.show()");
            }            
            
         }
         else
         {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Operació no permesa","Només professors de guardia en aquesta hora poden anotar substitucions."));   
         }
     }

    public String formataHores(java.sql.Time inicio, java.sql.Time fin)
    {
         return StringUtils.formatTime(inicio)+"-"+StringUtils.formatTime(fin);
    }
     
    public String triaSubstitucio() 
    {
        //System.out.println("debuggin triaSubstitucio ... selectedClase "+selectedClase+" and selectedincidencia "+selectedIncidencia);
        //System.out.println("profe "+selectedClase.getNombreProfesor());
        if(selectedClase==null) {
            return null;
        }
        if(selectedIncidencia==null) 
        {
             FacesContext context = FacesContext.getCurrentInstance();
             context.addMessage("Atenció:", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Cal que especifiqueu un tipus d'incidència." ));        
             return null;
        }
        
        //System.out.println("criadada a triasubstitucio amb idclase "+selectedClase.getIdClase());
               
                int selectedIncidenciaId = (int) Double.parseDouble(selectedIncidencia);
                
                if(selectedIncidenciaId<=0)
                {
                    //FacesMessage msg = new FacesMessage("Atenció", "Cal que especifiqueu un tipus d'incidència.");
                    //System.out.println("cridam a error");
                    FacesContext context = FacesContext.getCurrentInstance();
                    context.addMessage("Atenció:", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Cal que especifiqueu un tipus d'incidència." ));        
                    return null;
                }
                
                BeanTipoIncidencias inc = mapIncidencias.get(selectedIncidenciaId);
                //Desa la substitucio
                org.iesapp.clients.sgd7.profesores.FaltasProfesores fp = client.getSgdClient().getFaltasProfesores(selectedClase, selectedUser.getIdProfesor(), 
                            observacions, dia, inc.getId(), inc.getSimbolo(), inc.getDescripcion());
                int save = fp.save();
             
                RequestContext.getCurrentInstance().execute("incvar.hide()");
                
                  
                selectedClase.setBeanFProf(fp.getBean());
                
                //Afegeix una entrada a faltasprofesores
                substituciones.add(selectedClase);
                
                //System.out.println("redirecting-----");
                FacesUtil.sendRedirect("substitucions.jsf");
                return "substitucions";                
  
    }
    
    public String gotoLlista( )
    {
        if(selectedSubstitucion==null) {
            return null;
        }
        
        //Actualitza el mapa de la sessio i fa el redirect
        FacesUtil.setSessionMapValue("dia", dia);
        FacesUtil.setSessionMapValue("beanClase", selectedSubstitucion);     
        mbGllista pointer = (mbGllista) FacesUtil.getSessionMapValue("pointerGllista");
        if(pointer!=null) {
            pointer.updateUI();
        }
        return "gllista";                
    }
    
    public void delSubstitucio()
    {
        if(selectedSubstitucion==null) {
            return;
        }
        
        //System.out.println("hauria d'esborrar "+selectedSubstitucion.getIdClase());
        String SQL1 = "DELETE FROM faltasprofesores WHERE fecha='"+new DataCtrl(dia).getDataSQL()+"'"
                + " AND idProfesores="+selectedSubstitucion.getIdProfesor()
                + " AND idHorasCentro="+selectedSubstitucion.getIdHorasCentro(); 

        int nup = client.getIesClient().getSgd().executeUpdate(SQL1);
        //TORNA A CARREGAR
        int idDiaSetmana = new DataCtrl(dia).getIntDia();       
        substituciones = client.getSgdClient().getClasesCollection().getSubstitucionesByProfeGuardia(client.getIesClient().getSgd(), idDiaSetmana, dia, selectedUser.getIdProfesor());
   
        
    }

    public String gotoGuardies()
    {
        selectedClase = null;
        return "guardies";
    }
    public SelectItem[] getOpcionesHoras() {
        return opcionesHoras;
    }

    public Date getDia() {
        return dia;
    }

    public void setDia(Date dia) {
        this.dia = dia;
    }

    public String getSelectedHora() {
        return selectedHora;
    }

    public void setSelectedHora(String selectedHora) {
        this.selectedHora = selectedHora;
    }

    public ArrayList<BeanClaseGuardia> getHorario() {
        return horario;
    }

    public ArrayList<BeanClaseGuardia> getSubstituciones() {
        return substituciones;
    }

    public void setSubstituciones(ArrayList<BeanClaseGuardia> substituciones) {
        this.substituciones = substituciones;
    }

    public BeanClaseGuardia getSelectedSubstitucion() {
        return selectedSubstitucion;
    }

    public void setSelectedSubstitucion(BeanClaseGuardia selectedSubstitucion) {
        this.selectedSubstitucion = selectedSubstitucion;
    }

    public ArrayList<GuardiesBean> getOnduty() {
        return onduty;
    }

    public String getObservacions() {
        return observacions;
    }

    public void setObservacions(String observacions) {
        this.observacions = observacions;
    }

    public String getSelectedIncidencia() {
        return selectedIncidencia;
    }

    public void setSelectedIncidencia(String selectedIncidencia) {
        this.selectedIncidencia = selectedIncidencia;
    }

    public SelectItem[] getOpcionesIncidencias() {
        return opcionesIncidencias;
    }

    public BeanClaseGuardia getSelectedClase() {
        return selectedClase;
    }

    public void setSelectedClase(BeanClaseGuardia selectedClase) {
        this.selectedClase = selectedClase;
    }

    public boolean isAmionduty() {
        return amionduty;
    }

    /**
     * @return the pwdSU
     */
    public String getPwdSU() {
        return pwdSU;
    }

    /**
     * @param pwdSU the pwdSU to set
     */
    public void setPwdSU(String pwdSU) {
        this.pwdSU = pwdSU;
    }
}