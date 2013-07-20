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
import java.util.ArrayList;
import java.util.Calendar;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import org.iesapp.clients.iesdigital.dates.DatesControl;
import org.iesapp.clients.iesdigital.reserves.BeanRecursos;
import org.iesapp.clients.iesdigital.reserves.BeanReserves;
import org.iesapp.clients.iesdigital.reserves.BeanSelectHores;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.web.cloudws.FacesUtil;

@Named(value = "mbReserves")
//@SessionScoped
//@ManagedBean
@SessionScoped
//@ViewScoped
public class mbReserves implements java.io.Serializable {

    private BeanProfesor selectedUser;
    private ArrayList<BeanReserves> listReserves;
    private BeanReserves selectedReserva;
    private boolean showCaducada=false;
    private boolean onReserva = false;
    
    private String tipusRecurs="1"; //2=aules, 1=material
    private String selectedRecurs; //la id del recurs
    private ArrayList<BeanRecursos> listRecursos;
    private ArrayList<BeanSelectHores> listDispo;
    private java.util.Date diaReserva;
    private String tornReserva="1"; //1=mati, 2=tarda;
    private String horesTriades;
    private String motiuReserva="";
    private ArrayList<Integer> horesSelected;
    private int currentStep =1;
    private java.util.Date mindate;
    private java.util.Date maxdate;
    private String selectedRecursDesc;
    private final Client client;
    

    public mbReserves() {
        
        client = (Client) FacesUtil.getSessionMapValue("client");
   
        selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser");    
        
        listReserves = client.getIesClient().getReservesClient().getReservesCollection().
                       getReservesOf(selectedUser.getAbrev(), showCaducada, true, true, "");
                
        listRecursos = client.getIesClient().getReservesClient().getReservesCollection()
                .getRecursos(Integer.parseInt(tipusRecurs));
           
        java.util.Calendar cal = java.util.Calendar.getInstance();
        mindate = cal.getTime();
        String webXmlParam = (String) FacesUtil.getWebXmlParam("pdaweb.antelacioReserves");
        int antelacio = 15;
        try{
            antelacio = (int) Double.parseDouble(webXmlParam);
        }
        catch(java.lang.NumberFormatException e)
        {
            //System.out.println("Exception:"+e);
        }
        
        cal.add(Calendar.DAY_OF_YEAR, antelacio);
        maxdate =cal.getTime();
              
    } 

    public void loadReserves()
    {
        listReserves = client.getIesClient().getReservesClient().getReservesCollection().
                       getReservesOf(selectedUser.getAbrev(), showCaducada, true, true, "");
    }
    
    public void delete()
    {
        if(selectedReserva==null) {
            return;
        }
        int nup  = client.getIesClient().getReservesClient().getReservesCollection().delete(selectedReserva.getId());
        loadReserves();
        client.getIesClient().getCoreCfg().addAction("cancel-reserva");
    }
    
    public void onDateSelect(org.primefaces.event.SelectEvent ev)
    {
        diaReserva = (java.util.Date) ev.getObject();
        DatesControl cd = new DatesControl(diaReserva, client.getIesClient());
        if(cd.esFestiu())
        {
           FacesContext context = FacesContext.getCurrentInstance();
           while(context.getMessages().hasNext()) {
               context.getMessages().remove();
           }
           context.addMessage("msgcal", new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenció: ",
                   "Heu triat un dia no lectiu"));
        }
            
    }
    
    public String nova()
    {
        onReserva = true;
        currentStep = 1;
        //posa defecte aqui
        diaReserva = new java.util.Date();
        motiuReserva = "";
        selectedRecurs = null;
        tipusRecurs = "1";
        tornReserva = "1";
        return "reservesWiz";
    }
    
    public String cancelaWiz()
    {
        onReserva = false;
        return "reserves";
    }
    
     public void nextStep()
    {
        int avanca = 1;
        //System.out.println("i am in next");
        
        //comprova si pot avançar o no
        if(currentStep==1)
        {
            if(tipusRecurs==null || selectedRecurs==null || selectedRecurs.isEmpty())
            {
                avanca = 0;
                FacesContext context = FacesContext.getCurrentInstance();
                while(context.getMessages().hasNext()) {
                    context.getMessages().remove();
                }
                context.addMessage("msgrecurs", new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenció: ", "Cal que trieu un recurs"));
            }
        }
        else if(currentStep==2)
        {
            if(tornReserva==null || diaReserva==null)
            {
                avanca = 0;
                tornReserva="1";
            }
            else
            {
                listDispo = client.getIesClient().getReservesClient().getReservesCollection().listDisponibilitat(diaReserva, tornReserva, Integer.parseInt(tipusRecurs), selectedRecurs);
            }
        }
        else if(currentStep==3)
        {
           //hi ha d'haver almenys una hora triada sino nada
           horesSelected = new ArrayList<Integer>();
           for(int i=0; i<listDispo.size(); i++)
            {
                if(listDispo.get(i).isTriat())
                {
                  horesSelected.add(listDispo.get(i).getIdHora());
                }
            }
            
           horesTriades = "";
           
           for(int i=0; i<horesSelected.size();i++)
           {
               horesTriades += horesSelected.get(i)+"a; ";
           }
                   
            
            for(int i=0; i<listRecursos.size(); i++)
            {
                if(listRecursos.get(i).getId().equals(selectedRecurs))
                {
                    selectedRecursDesc = listRecursos.get(i).getDescripcio();
                    break;
                }
            }
            
            if(horesSelected.isEmpty())
            {
               avanca =0;
               FacesContext context = FacesContext.getCurrentInstance();
               while(context.getMessages().hasNext()) {
                    context.getMessages().remove();
                }
               context.addMessage("msghores", new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenció: ", "Cal que trieu alguna hora"));
            }
        }
        
        if(currentStep<4) {
            currentStep +=avanca;
        }
    }
    
    public void backStep()
    {
        //System.out.println("i am in back");
        if(currentStep>1) {
            currentStep -=1;
        }   
    }
    
    public void updateDispo()
    {
        listDispo = client.getIesClient().getReservesClient().getReservesCollection().listDisponibilitat(diaReserva, tornReserva, Integer.parseInt(tipusRecurs), selectedRecurs);
    }
    
    public String doReserva()
    {
        //System.out.println("datos reserva:: "+ selectedUser.getAbrev()+ tipusRecurs+ selectedRecurs+ diaReserva);
        //System.out.println("la reserva s'efectuara a les hores"+horesSelected.toString());
        client.getIesClient().getReservesClient().getReservesCollection().makeReserva(selectedUser.getAbrev(), tipusRecurs, selectedRecurs, diaReserva, horesSelected, motiuReserva);
        
        client.getIesClient().getCoreCfg().addAction("make-reserva");
        loadReserves();
        onReserva = false;
        return "reserves";
    }
    
    public void updateRecursos()
    {
        //System.out.println("Estic fent un update");
         listRecursos = client.getIesClient().getReservesClient().getReservesCollection()
                .getRecursos(Integer.parseInt(tipusRecurs));

    }
    
    /**
     * @return the listReserves
     */
    public ArrayList<BeanReserves> getListReserves() {
        return listReserves;
    }

    /**
     * @param listReserves the listReserves to set
     */
    public void setListReserves(ArrayList<BeanReserves> listReserves) {
        this.listReserves = listReserves;
    }

    /**
     * @return the selectedReserva
     */
    public BeanReserves getSelectedReserva() {
        return selectedReserva;
    }

    /**
     * @param selectedReserva the selectedReserva to set
     */
    public void setSelectedReserva(BeanReserves selectedReserva) {
        this.selectedReserva = selectedReserva;
    }

    /**
     * @return the showCaducada
     */
    public boolean isShowCaducada() {
        return showCaducada;
    }

    /**
     * @param showCaducada the showCaducada to set
     */
    public void setShowCaducada(boolean showCaducada) {
        this.showCaducada = showCaducada;
    }

    /**
     * @return the onReserva
     */
    public boolean isOnReserva() {
        return onReserva;
    }

    /**
     * @param onReserva the onReserva to set
     */
    public void setOnReserva(boolean onReserva) {
        this.onReserva = onReserva;
    }

    /**
     * @return the tipusRecurs
     */
    public String getTipusRecurs() {
        return tipusRecurs;
    }

    /**
     * @param tipusRecurs the tipusRecurs to set
     */
    public void setTipusRecurs(String tipusRecurs) {
        this.tipusRecurs = tipusRecurs;
    }
 
    /**
     * @return the selectedRecurs
     */
    public String getSelectedRecurs() {
        return selectedRecurs;
    }

    /**
     * @param selectedRecurs the selectedRecurs to set
     */
    public void setSelectedRecurs(String selectedRecurs) {
        this.selectedRecurs = selectedRecurs;
    }

    /**
     * @return the listRecursos
     */
    public ArrayList<BeanRecursos> getListRecursos() {
        return listRecursos;
    }

    /**
     * @param listRecursos the listRecursos to set
     */
    public void setListRecursos(ArrayList<BeanRecursos> listRecursos) {
        this.listRecursos = listRecursos;
    }

    /**
     * @return the diaReserva
     */
    public java.util.Date getDiaReserva() {
        return diaReserva;
    }

    /**
     * @param diaReserva the diaReserva to set
     */
    public void setDiaReserva(java.util.Date diaReserva) {
        this.diaReserva = diaReserva;
    }

    /**
     * @return the tornReserva
     */
    public String getTornReserva() {
        return tornReserva;
    }

    /**
     * @param tornReserva the tornReserva to set
     */
    public void setTornReserva(String tornReserva) {
        this.tornReserva = tornReserva;
    }

    /**
     * @return the listDispo
     */
    public ArrayList<BeanSelectHores> getListDispo() {
        return listDispo;
    }

    /**
     * @param listDispo the listDispo to set
     */
    public void setListDispo(ArrayList<BeanSelectHores> listDispo) {
        this.listDispo = listDispo;
    }

    /**
     * @return the horesTriades
     */
    public String getHoresTriades() {
        return horesTriades;
    }

    /**
     * @param horesTriades the horesTriades to set
     */
    public void setHoresTriades(String horesTriades) {
        this.horesTriades = horesTriades;
    }

    /**
     * @return the currentStep
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * @param currentStep the currentStep to set
     */
    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    /**
     * @return the mindate
     */
    public java.util.Date getMindate() {
        return mindate;
    }

    /**
     * @return the maxdate
     */
    public java.util.Date getMaxdate() {
        return maxdate;
    }

    /**
     * @return the motiuReserva
     */
    public String getMotiuReserva() {
        return motiuReserva;
    }

    /**
     * @param motiuReserva the motiuReserva to set
     */
    public void setMotiuReserva(String motiuReserva) {
        this.motiuReserva = motiuReserva;
    }

    /**
     * @return the selectedRecursDesc
     */
    public String getSelectedRecursDesc() {
        return selectedRecursDesc;
    }

    /**
     * @param selectedRecursDesc the selectedRecursDesc to set
     */
    public void setSelectedRecursDesc(String selectedRecursDesc) {
        this.selectedRecursDesc = selectedRecursDesc;
    }

    
    
}