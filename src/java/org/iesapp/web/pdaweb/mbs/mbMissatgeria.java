/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

import org.iesapp.core.util.Client;
import java.util.ArrayList;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import org.iesapp.clients.iesdigital.missatgeria.BeanMissatge;
import org.iesapp.clients.iesdigital.missatgeria.MissatgeriaCollection;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.util.StringUtils;
import org.iesapp.web.cloudws.FacesUtil;
 
/**
 *
 * @author Josep
 */
//@Named()
//@SessionScoped
@ManagedBean(name="mbMissatgeria")
//@SessionScoped
@ViewScoped
public class mbMissatgeria implements java.io.Serializable {
    
    private ArrayList<BeanMissatge> listPendents;
    private ArrayList<BeanMissatge> listEnviats;
    protected BeanMissatge pendentSelected;
    private final BeanProfesor selectedUser;
    protected String selectedActitud="";
    protected String selectedFeina="";
    protected String selectedNotes="";
    protected String selectedObservacions="";
    private int currentPendent;
    private int numPendents = 0;
    protected ArrayList<SelectItem> comboActitud;
    protected ArrayList<SelectItem> comboNotes;
    protected ArrayList<SelectItem> comboFeina;
    protected ArrayList<SelectItem> comboObservacions;
    private BeanMissatge enviatSelected;
    private final Client client;

    
    
    public mbMissatgeria()
    {
        
         selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser");  
         client = (Client) FacesUtil.getSessionMapValue("client");
        
         listPendents = client.getIesClient().getMissatgeriaCollection().listSolicitudsPendents();
         listEnviats = client.getIesClient().getMissatgeriaCollection().loadEnviats("ORDER BY dataContestat DESC");
   
         comboActitud = toSelectItemList(client.getIesClient().getMissatgeriaCollection().listMissatgeriaItems(MissatgeriaCollection.ITEM_ACTITUD));
         comboFeina = toSelectItemList(client.getIesClient().getMissatgeriaCollection().listMissatgeriaItems(MissatgeriaCollection.ITEM_FEINA));
         comboNotes = toSelectItemList(client.getIesClient().getMissatgeriaCollection().listMissatgeriaItems(MissatgeriaCollection.ITEM_NOTES));
         comboObservacions = toSelectItemList(client.getIesClient().getMissatgeriaCollection().listMissatgeriaItems(MissatgeriaCollection.ITEM_OBSERVACIONS));
            
         pendentSelected = new BeanMissatge();
         currentPendent = 0;
         numPendents = listPendents.size();
         if(numPendents>0)
         {
             pendentSelected = listPendents.get(currentPendent);
         
         }
         
    }

    public void back()
    {
        int tmp = currentPendent - 1;
        int nn = listPendents.size();
        if(tmp>=0 && tmp<nn)
        {
            //Desa a la llista l'actual, que pot haver estat modificat  
            listPendents.set(currentPendent, pendentSelected.clone());  
        
            currentPendent -=1;
            pendentSelected = listPendents.get(currentPendent);
        }
    }
    
    public void next()
    { 
        int tmp = currentPendent + 1;
        int nn = listPendents.size();
        if(tmp>=0 && tmp<nn)
        {
            //Desa a la llista l'actual, que pot haver estat modificat
            listPendents.set(currentPendent,  pendentSelected.clone() );
        
        
            currentPendent +=1;
            pendentSelected = listPendents.get(currentPendent);
        }
        
    }
    
    public void send()
    {
        //First check fields
        if(pendentSelected.getActitud().trim().isEmpty())
        {
               FacesContext.getCurrentInstance().addMessage("growl", 
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "El camp 'actitud' no pot ésser buit.",""));
               return;
        }
        if(pendentSelected.getFeina().trim().isEmpty())
        {
               FacesContext.getCurrentInstance().addMessage("growl", 
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "El camp 'feina' no pot ésser buit.",""));
               return;
        }
        if(pendentSelected.getNotes().trim().isEmpty())
        {
               FacesContext.getCurrentInstance().addMessage("growl", 
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "El camp 'notes' no pot ésser buit.",""));
               return;
        }
        
        int nup = client.getIesClient().getMissatgeriaCollection().saveBeanMissatge(pendentSelected);
        client.getIesClient().getCoreCfg().addAction("send-missatgeria");
       
        if(nup>0)
        {
            FacesContext.getCurrentInstance().addMessage("growl", 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "S'ha enviat la informació al tutor/a",""));
            
            listPendents.remove(currentPendent);
            numPendents = listPendents.size();
            currentPendent = 0;
            if(numPendents>0)
            {
                pendentSelected = listPendents.get(currentPendent);
            }
            
            //Ha de tornar a carregar la llista de enviats
            listEnviats = client.getIesClient().getMissatgeriaCollection().loadEnviats("ORDER BY dataContestat DESC");
        }
        
    }
    
    public void comboEvent(String idCombo)
    {
        if(idCombo.equals("actitud"))
        {
            for(int i=0; i<comboActitud.size();i++)
            {
                String id1 = (String) comboActitud.get(i).getValue();
               
                if(selectedActitud.equals(id1))
                {
                    String tmp = StringUtils.noNull( listPendents.get(currentPendent).getActitud() );
                    String txt = tmp+" "+ comboActitud.get(i).getLabel() +";";
                    listPendents.get(currentPendent).setActitud(txt);
                    pendentSelected = listPendents.get(currentPendent);
                    break;
                }
            }
        }
        else if(idCombo.equals("feina"))
        {
            for(int i=0; i<comboFeina.size();i++)
            {
                String id1 = (String) comboFeina.get(i).getValue();
               
                if(selectedFeina.equals(id1))
                {
                    String tmp = StringUtils.noNull( listPendents.get(currentPendent).getFeina() );
                    String txt = tmp+" "+ comboFeina.get(i).getLabel() +";";
                    listPendents.get(currentPendent).setFeina(txt);
                    pendentSelected = listPendents.get(currentPendent);
                    break;
                }
            }
        }
        else if(idCombo.equals("notes"))
        {
            for(int i=0; i<comboNotes.size();i++)
            {
                String id1 = (String) comboNotes.get(i).getValue();
               
                if(selectedNotes.equals(id1))
                {
                    String tmp = StringUtils.noNull( listPendents.get(currentPendent).getNotes() );
                    String txt = tmp+" "+ comboNotes.get(i).getLabel() +";";
                    listPendents.get(currentPendent).setNotes(txt);
                    pendentSelected = listPendents.get(currentPendent);
                    break;
                }
            }
        }
        else if(idCombo.equals("observacions"))
        {
            for(int i=0; i<comboObservacions.size();i++)
            {
                String id1 = (String) comboObservacions.get(i).getValue();
               
                if(selectedObservacions.equals(id1))
                {
                    String tmp = StringUtils.noNull( listPendents.get(currentPendent).getComentari() );
                    String txt = tmp+" "+ comboObservacions.get(i).getLabel() +";";
                    listPendents.get(currentPendent).setComentari(txt);
                    pendentSelected = listPendents.get(currentPendent);
                    break;
                }
            }
        }
        
        if(!idCombo.isEmpty()) {
            selectedActitud = "";
        }
    }
            
     
    public void reedita(int id)
    {
       
        for(int i=0; i<listEnviats.size(); i++)
        {
            if(id==listEnviats.get(i).getId())
            {
                enviatSelected = listEnviats.get(i);
                break;
            }
        }
    }
    
    
    public void sendAgain()
    {
        client.getIesClient().getCoreCfg().addAction("sendAgain-missatgeria");
      
        if(enviatSelected==null) {
            return;
        }
        int nup = client.getIesClient().getMissatgeriaCollection().saveBeanMissatge(pendentSelected);
        
        if(nup>0)
        {
            FacesContext.getCurrentInstance().addMessage("growl", 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "S'ha actualitzat la informació per al tutor/a",""));
        }
    }
    
    public void dismissExpired()
    {
       pendentSelected.setActitud(pendentSelected.getActitud()+" : Sol.licitud caducada - no contestat -");
       pendentSelected.setFeina(pendentSelected.getFeina()+" : Sol.licitud caducada - no contestat -");
       pendentSelected.setNotes(pendentSelected.getNotes()+" : Sol.licitud caducada - no contestat -");
          
       pendentSelected.setComentari(pendentSelected.getComentari()+" : Sol.licitud caducada - no contestat -");
       int nup = client.getIesClient().getMissatgeriaCollection().saveBeanMissatge(pendentSelected);
       client.getIesClient().getCoreCfg().addAction("send-missatgeria");
       
       if(nup>0)
       {
            FacesContext.getCurrentInstance().addMessage("growl", 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "S'ha eliminat el missatge caducat",""));
            
            listPendents.remove(currentPendent);
            numPendents = listPendents.size();
            currentPendent = 0;
            if(numPendents>0)
            {
              pendentSelected = listPendents.get(currentPendent);
            }
            //Ha de tornar a carregar la llista de enviats
            listEnviats = client.getIesClient().getMissatgeriaCollection().loadEnviats("ORDER BY dataContestat DESC");
        }
        
    }
    /**
     * @return the pendentSelected
     */
    public BeanMissatge getPendentSelected() {
        return pendentSelected;
    }

    /**
     * @param pendentSelected the pendentSelected to set
     */
    public void setPendentSelected(BeanMissatge pendentSelected) {
        this.pendentSelected = pendentSelected;
    }

    /**
     * @return the selectedActitud
     */
    public String getSelectedActitud() {
        return selectedActitud;
    }

    /**
     * @param selectedActitud the selectedActitud to set
     */
    public void setSelectedActitud(String selectedActitud) {
        this.selectedActitud = selectedActitud;
    }

    /**
     * @return the selectedFeina
     */
    public String getSelectedFeina() {
        return selectedFeina;
    }

    /**
     * @param selectedFeina the selectedFeina to set
     */
    public void setSelectedFeina(String selectedFeina) {
        this.selectedFeina = selectedFeina;
    }

    /**
     * @return the selectedNotes
     */
    public String getSelectedNotes() {
        return selectedNotes;
    }

    /**
     * @param selectedNotes the selectedNotes to set
     */
    public void setSelectedNotes(String selectedNotes) {
        this.selectedNotes = selectedNotes;
    }

    /**
     * @return the selectedObservacions
     */
    public String getSelectedObservacions() {
        return selectedObservacions;
    }

    /**
     * @param selectedObservacions the selectedObservacions to set
     */
    public void setSelectedObservacions(String selectedObservacions) {
        this.selectedObservacions = selectedObservacions;
    }

    /**
     * @return the numPendents
     */
    public int getNumPendents() {
        return numPendents;
    }

    /**
     * @param numPendents the numPendents to set
     */
    public void setNumPendents(int numPendents) {
        this.numPendents = numPendents;
    }

    /**
     * @return the comboActitud
     */
    public ArrayList<SelectItem> getComboActitud() {
        return comboActitud;
    }

    /**
     * @return the comboNotes
     */
    public ArrayList<SelectItem> getComboNotes() {
        return comboNotes;
    }

    /**
     * @return the comboFeina
     */
    public ArrayList<SelectItem> getComboFeina() {
        return comboFeina;
    }

    /**
     * @return the comboObservacions
     */
    public ArrayList<SelectItem> getComboObservacions() {
        return comboObservacions;
    }

    public ArrayList<BeanMissatge> getListEnviats() {
        return listEnviats;
    }

    public BeanMissatge getEnviatSelected() {
        return enviatSelected;
    }

    public void setEnviatSelected(BeanMissatge enviatSelected) {
        this.enviatSelected = enviatSelected;
    }

    private ArrayList<SelectItem> toSelectItemList(ArrayList<String> list) {
       ArrayList<SelectItem> list2 = new ArrayList<SelectItem>();
       int i = 0;
       for(String s: list)
       {
           list2.add(new SelectItem(i, s));
           i += 1;
       }
       return list2;
    }
    
    
}
