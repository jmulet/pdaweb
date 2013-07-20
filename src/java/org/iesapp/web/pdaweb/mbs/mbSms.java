/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

/**
 *
 * @author Josep
 */
 
import org.iesapp.web.cloudws.FileInfo;
import org.iesapp.web.cloudws.GenericResource;
import org.iesapp.core.util.Client;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.iesapp.clients.sgd7.mensajes.BeanInbox;
import org.iesapp.clients.sgd7.mensajes.BeanMensajesAttachment;
import org.iesapp.clients.sgd7.mensajes.BeanOutbox;
import org.iesapp.clients.sgd7.mensajes.BeanProfSms;
import org.iesapp.clients.sgd7.mensajes.MensajesListas;
import org.iesapp.clients.sgd7.mensajes.MensajesListasProfesores;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.util.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.DualListModel;
import org.primefaces.model.UploadedFile;
import org.iesapp.web.cloudws.FacesUtil;
import org.iesapp.web.cloudws.Html2Text;
import org.iesapp.web.cloudws.Uploader;
//
//@Named(value = "mbSms")
@ManagedBean(name = "mbSms")
@SessionScoped


public class mbSms implements java.io.Serializable {

    private BeanProfesor selectedUser;
    private boolean renderInbox=true;
    private boolean renderTrash=false;
    private boolean renderNou=false;
    private boolean renderOutbox=false;
    private boolean allprofs=false;
    
    private BeanInbox inboxSelected;
    private BeanInbox trashSelected;
    private BeanOutbox outboxSelected;
    
    private ArrayList<BeanInbox> listTrash;
    private ArrayList<BeanInbox> listInbox;
    private ArrayList<BeanOutbox> listOutbox; 
    
    private BeanProfSms selectedProf;
    private String nouSmsText="";
    private ArrayList<BeanProfSms> professors;
    protected ArrayList<BeanProfSms> filteredProfessors;
    protected BeanProfSms[] selectedProfessors;
    private int noleidos;
    private String perA="";
    private static final int BUFSIZE = 4096;
    protected DualListModel<BeanProfSms> dualList;
    protected ArrayList<BeanMensajesAttachment> attachments;
    private String fileHostingDir;
    private ArrayList<String> equipDocent;
    protected boolean selectEquipDocent;
    protected final SelectItem[] opcionesListas;
    protected String selectedListas="0";
    private final ArrayList<MensajesListas> mensajesListas;
    private final Client client;
     

    public mbSms() {
        
        selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser");
        client = (Client) FacesUtil.getSessionMapValue("client");
        //Carrega l'inbox
        renderInbox = true;
        renderTrash = false;
        
        listInbox = client.getSgdClient().getMensajesCollection().getInbox(selectedUser.getIdProfesor(),false);
        listTrash = client.getSgdClient().getMensajesCollection().getInbox(selectedUser.getIdProfesor(),true);
        listOutbox = client.getSgdClient().getMensajesCollection().getOutbox(selectedUser.getIdProfesor());
        
        //Combo llista
        mensajesListas = client.getSgdClient().getMensajesCollection().getMensajesListas();
        int nn = mensajesListas.size();
        opcionesListas = new SelectItem[nn];
        int i = 0;
        for(MensajesListas ml: mensajesListas)
        {
            opcionesListas[i]= new SelectItem(ml.getId(), ml.getNombre());
            i += 1;
        }
        
        
        professors = client.getSgdClient().getProfesoresCollection().listProf();
        filteredProfessors = new ArrayList<BeanProfSms>();
        dualList = new DualListModel<BeanProfSms>(professors, new ArrayList<BeanProfSms>());
        attachments = new ArrayList<BeanMensajesAttachment>();
        noleidos = client.getSgdClient().getMensajesCollection().getSmsNoLeidos(selectedUser.getIdProfesor());
        FacesUtil.setSessionMapValue("newMail", noleidos);
        
       
      
        fileHostingDir = StringUtils.noNull((String) FacesUtil.getWebXmlParam("pdaweb.fileHostingDir")).trim();
        if (!fileHostingDir.endsWith(File.separator)) {
            fileHostingDir += File.separator;
        }
        GenericResource.checkIntegrity(fileHostingDir, selectedUser.getSystemUser(), client.getIesClient().getCoreCfg().anyAcademic);
    }

    public void enviaSms()
    {
        //Comprovacions
        if(dualList.getTarget().isEmpty())
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,"Cap destinatari",""));
            return;
        }
        //Comprovacions
        if(nouSmsText==null || nouSmsText.trim().isEmpty())
        {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,"Cap missatge",""));
            return;
        }
        
         
        String richText = nouSmsText;
        Html2Text html2Text = new Html2Text();
        try {
            html2Text.parse(new StringReader(richText));
        } catch (IOException ex) {
            Logger.getLogger(mbSms.class.getName()).log(Level.SEVERE, null, ex);
        }
        String plainText = html2Text.getText();
        for(BeanProfSms bean: dualList.getTarget())
        {
            bean.setSelected(true);
        }
        client.getSgdClient().getMensajesCollection().sendSms(selectedUser.getIdProfesor(), dualList.getTarget(), plainText, richText, getAttachments());
       
        //Reset new sms form
        dualList = new DualListModel<BeanProfSms>(professors, new ArrayList<BeanProfSms>());
        setAttachments(new ArrayList<BeanMensajesAttachment>());
        selectedListas="0";
        selectEquipDocent = false;
        nouSmsText = "";
        switcher(0);
        client.getIesClient().getCoreCfg().addAction("send-sms");
           
    }
    
    public void switcher(int id)
    {

        switch(id)
        {
            case 0: renderInbox = true; renderOutbox=false; setRenderTrash(false); renderNou=false; setListInbox(client.getSgdClient().getMensajesCollection().getInbox( selectedUser.getIdProfesor(),false)); break;
            
            case 1: renderInbox = false; renderOutbox=true;  setRenderTrash(false); renderNou=false; setListOutbox(client.getSgdClient().getMensajesCollection().getOutbox( selectedUser.getIdProfesor())); break;
                
            case 2: renderInbox = false; renderOutbox=false; setRenderTrash(true); renderNou=false; setListTrash(client.getSgdClient().getMensajesCollection().getInbox( selectedUser.getIdProfesor(),true)); break;                
                
            case 3: renderInbox = false; renderOutbox=false; setRenderTrash(false); renderNou=true;  
                    nouSmsText ="";
                    for(int i=0; i<professors.size();i++)
                    {professors.get(i).setSelected(false);}                   
                    dualList = new DualListModel<BeanProfSms>(professors, new ArrayList<BeanProfSms>());
                    setAttachments(new ArrayList<BeanMensajesAttachment>());
                    selectedListas="0";
                    selectEquipDocent = false;
                    break;                                
        }
        
    }
    
    public void onMove2Trash()
    {
        //System.out.println("move to trash");
        //he d'agafar la selectedInc
       
        if (inboxSelected == null) {
            return;
        }

        //canvia a la base
        int nup = client.getSgdClient().getMensajesCollection().setBorradoUpProfe(inboxSelected.getId());
        //canvia a la llista
        setListInbox(client.getSgdClient().getMensajesCollection().getInbox(selectedUser.getIdProfesor(),false));
        client.getIesClient().getCoreCfg().addAction("del-sms");
    }
    
     public void onMove2Inbox()
    {
        //System.out.println("move to inbox");
        //he d'agafar la selectedInc
       
        if (trashSelected == null) {
            return;
        }

        //canvia a la base
        int nup = client.getSgdClient().getMensajesCollection().removeBorradoUpProfe(trashSelected.getId());
        //canvia a la llista
        setListTrash(client.getSgdClient().getMensajesCollection().getInbox(selectedUser.getIdProfesor(),true));
    }
    
     public String replay()
     {
         if(inboxSelected == null) {
             return null;
         }
         
         String desti = inboxSelected.getIdremitente()+"";
         String remite = selectedUser.getIdProfesor();
         int idSms = inboxSelected.getId();
         
         boolean goMissatgeria = false;
         //Determina si aquest missatge correspon a un de petició d'informacio de tutors
         String SQL1 = "Select * from sig_missatgeria where idMensajeProfesor="+idSms;
         
         try {
             Statement st = client.getIesClient().getMysql().createStatement();
             ResultSet rs1 = client.getIesClient().getMysql().getResultSet(SQL1,st);
             
            if(rs1!=null && rs1.next())
            {
                goMissatgeria = true;
            }
            rs1.close();
            st.close();
         } catch (SQLException ex) {
            Logger.getLogger(mbSms.class.getName()).log(Level.SEVERE, null, ex);
         }
         
         if(goMissatgeria)
         {
               return "missatgeria.xhtml?faces-redirect=true";
         } 
         
         for(int i=0; i<professors.size(); i++)
             {
                 if(professors.get(i).getCodigo().equals(desti)) {
                     professors.get(i).setSelected(true);
                 }
                 else {
                     professors.get(i).setSelected(false);
                 }
             }

          renderInbox = false; renderOutbox=false; renderTrash=false; renderNou=true;  
          nouSmsText ="En resposta a ";
          perA = inboxSelected.getRemitente();
          //El dual list s'ha d'actualizar per posar perA a target
          //Per seguritat tornam a definir el dual list
          dualList = new DualListModel<BeanProfSms>(professors, new ArrayList<BeanProfSms>());
          BeanProfSms pointer = null;
          for(BeanProfSms bean: dualList.getSource())
          {
                if(bean.getCodigo().equals(inboxSelected.getIdremitente()+""))
                {
                    pointer = bean;
                    break;
                }
          }
          if(pointer!=null)
          {
              dualList.getTarget().add(pointer);
          }
    
          return null;
     }
        
     public void checkLeido(org.primefaces.event.ToggleEvent ev)
     {
         
        //Carrega el nou missatge
        inboxSelected = (BeanInbox) ev.getData();
        //System.out.println("check leido is...."+inboxSelected.getTexto());
        if(inboxSelected==null) {
             return;
         }
        
        if(inboxSelected!=null && inboxSelected.getRichText()==null)
        {
            String loadRichText = client.getSgdClient().getMensajesCollection().loadRichText(inboxSelected.getTexto(), inboxSelected.getIdMensaje());
            inboxSelected.setRichText(loadRichText);
        }
           
        //Això es necessari perque amagi el missatge anteriorment clicat
        if(inboxSelected != null)
        {
            inboxSelected.setSelected(false);
        }
         
        
        
        //ha de mostrar tot el missatge
        inboxSelected.setSelected(true);
        
        if(inboxSelected != null && !inboxSelected.isLeido())
        {
            //System.out.println("inboxSelected "+inboxSelected.getId());
            //canvia a la base
            int nup = client.getSgdClient().getMensajesCollection().setLeido(inboxSelected.getId());
            //canvia a la llista
            //setListInbox(MensajesCollection.getInbox(selectedUser.getIdProfesor(),false));
            noleidos = client.getSgdClient().getMensajesCollection().getSmsNoLeidos(selectedUser.getIdProfesor());
            FacesUtil.setSessionMapValue("newMail", noleidos);
            //System.out.println("noleidos"+noleidos);
            int myid = inboxSelected.getId();
            for(int i=0; i<listInbox.size();i++)
            {
                if(listInbox.get(i).getId()==myid)
                {
                    listInbox.get(i).setLeido(true);
                    inboxSelected.setLeido(true);
                    break;
                }
            }
            client.getIesClient().getCoreCfg().addAction("read-sms");
        }
       
     }
 
    public void onSelectProf(SelectEvent event)
    {
      BeanProfSms bsms =  (BeanProfSms) event.getObject();
      if(bsms!=null)
      {
           bsms.setSelected(true);
      }
      updateDestinataris();
    }
    
    public void onUnselectProf(UnselectEvent event)
    {
        BeanProfSms bsms =  (BeanProfSms) event.getObject();
        if(bsms!=null)
        {
          bsms.setSelected(false);
        }
        updateDestinataris();
    }
    
    private void updateDestinataris()
    {
       
        String txt = "";
        int n=0;
        for(int i=0; i<professors.size();i++)
        {
            if(professors.get(i).isSelected())
            {
              txt += professors.get(i).getNombre()+";\n";
              n += 1;
            }
            
        }
        
        if(n==professors.size()) {
            perA = "<tots els professors>";
        }
        else {
            perA = txt;
        }
    }
     
    public void onallprof()
    {
               
        for(int i=0; i<professors.size();i++)
        {
            professors.get(i).setSelected(allprofs);
            
        }
        
        if(allprofs) {            
            perA = "<tots els professors>";
        }
        else {
            perA = "";
        }
    }
    
    public String retalla(String str, int max)
    {
        String txt;
        int len = str.length();
        if(len<max) { 
            txt = str;
        }
        else {
            txt = str.substring(0, max)+"...";
        }
        
        return txt;
              
    }
    
    /**
     * @return the renderInbox
     */
    public boolean isRenderInbox() {
        return renderInbox;
    }

    /**
     * @param renderInbox the renderInbox to set
     */
    public void setRenderInbox(boolean renderInbox) {
        this.renderInbox = renderInbox;
    }

    /**
     * @return the listInbox
     */
    public ArrayList<BeanInbox> getListInbox() {
        return listInbox;
    }

    /**
     * @param listInbox the listInbox to set
     */
    public void setListInbox(ArrayList<BeanInbox> listInbox) {
        this.listInbox=listInbox;
    }

    /**
     * @return the inboxSelected
     */
    public BeanInbox getInboxSelected() {
        return inboxSelected;
    }

    /**
     * @param inboxSelected the inboxSelected to set
     */
    public void setInboxSelected(BeanInbox inboxSelected) {
        this.inboxSelected = inboxSelected;
    }

    /**
     * @return the listTrash
     */
    public ArrayList<BeanInbox> getListTrash() {
        return listTrash;
    }

    /**
     * @return the renderTrash
     */
    public boolean isRenderTrash() {
        return renderTrash;
    }

    /**
     * @param renderTrash the renderTrash to set
     */
    public void setRenderTrash(boolean renderTrash) {
        this.renderTrash = renderTrash;
    }

    /**
     * @return the trashSelected
     */
    public BeanInbox getTrashSelected() {
        return trashSelected;
    }

    /**
     * @param trashSelected the trashSelected to set
     */
    public void setTrashSelected(BeanInbox trashSelected) {
        this.trashSelected = trashSelected;
    }
 

    /**
     * @return the renderNou
     */
    public boolean isRenderNou() {
        return renderNou;
    }

    /**
     * @param renderNou the renderNou to set
     */
    public void setRenderNou(boolean renderNou) {
        this.renderNou = renderNou;
    }

   
    /**
     * @return the nouSmsText
     */
    public String getNouSmsText() {
        return nouSmsText;
    }

    /**
     * @param nouSmsText the nouSmsText to set
     */
    public void setNouSmsText(String nouSmsText) {
        this.nouSmsText = nouSmsText;
    }

    /**
     * @return the selectedProf
     */
    public BeanProfSms getSelectedProf() {
        return selectedProf;
    }

    /**
     * @param selectedProf the selectedProf to set
     */
    public void setSelectedProf(BeanProfSms selectedProf) {
        this.selectedProf = selectedProf;
    }

    /**
     * @return the professors
     */
    public ArrayList<BeanProfSms> getProfessors() {
        return professors;
    }

    /**
     * @param professors the professors to set
     */
    public void setProfessors(ArrayList<BeanProfSms> professors) {
        this.professors = professors;
    }

    /**
     * @return the renderOutbox
     */
    public boolean isRenderOutbox() {
        return renderOutbox;
    }

    /**
     * @param renderOutbox the renderOutbox to set
     */
    public void setRenderOutbox(boolean renderOutbox) {
        this.renderOutbox = renderOutbox;
    }

    /**
     * @return the listOutbox
     */
    public ArrayList<BeanOutbox> getListOutbox() {
        return listOutbox;
    }

    /**
     * @param listOutbox the listOutbox to set
     */
    public void setListOutbox(ArrayList<BeanOutbox> listOutbox) {
        this.listOutbox=listOutbox;
    }

    /**
     * @return the outboxSelected
     */
    public BeanOutbox getOutboxSelected() {
        return outboxSelected;
    }

    /**
     * @param outboxSelected the outboxSelected to set
     */
    public void setOutboxSelected(BeanOutbox outboxSelected) {
        this.outboxSelected = outboxSelected;
    }

    /**
     * @param listTrash the listTrash to set
     */
    public void setListTrash(ArrayList<BeanInbox> listTrash) {
        this.listTrash = listTrash;
    }

    /**
     * @return the noleidos
     */
    public int getNoleidos() {
        return noleidos;
    }

    /**
     * @param noleidos the noleidos to set
     */
    public void setNoleidos(int noleidos) {
        this.noleidos = noleidos;
    }

    /**
     * @return the allprofs
     */
    public boolean isAllprofs() {
        return allprofs;
    }

    /**
     * @param allprofs the allprofs to set
     */
    public void setAllprofs(boolean allprofs) {
        this.allprofs = allprofs;
    }

     /**
     * @return the perA
     */
    public String getPerA() {
        return perA;
    }

    /**
     * @param perA the perA to set
     */
    public void setPerA(String perA) {
        this.perA = perA;
    }


    public void onShowDocument(String doc){
            //Intentem utilitzar (*)
            FacesContext fcontext = FacesContext.getCurrentInstance();
            ExternalContext econtext = fcontext.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) econtext.getResponse();
            //HttpServletRequest request = (HttpServletRequest) econtext.getRequest();
           
            response.setContentType("application/octet-stream");
            response.setHeader("Content-disposition", "inline; filename=\""
 					+ StringUtils.AfterLast(doc, File.separator) + "\"");
            
            File file = new File(fileHostingDir+doc);
            int length = (int) file.length();
            response.setContentLength(length);
            try{
            ServletOutputStream outStream = response.getOutputStream();
            byte[] byteBuffer = new byte[BUFSIZE];
            DataInputStream in = new DataInputStream(new FileInputStream(file));

            // reads the file's bytes and writes them to the response stream
            while ((in != null) && ((length = in.read(byteBuffer)) != -1)) {
                outStream.write(byteBuffer, 0, length);
            }
            in.close();
            outStream.close();
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
            fcontext.responseComplete();
    }
    
    

    public ArrayList<BeanProfSms> getFilteredProfessors() {
        return filteredProfessors;
    }

    public void setFilteredProfessors(ArrayList<BeanProfSms> filteredProfessors) {
        this.filteredProfessors = filteredProfessors;
    }

    public BeanProfSms[] getSelectedProfessors() {
        return selectedProfessors;
    }

    public void setSelectedProfessors(BeanProfSms[] selectedProfessors) {
        this.selectedProfessors = selectedProfessors;
    }

    public DualListModel<BeanProfSms> getDualList() {
        return dualList;
    }

    public void setDualList(DualListModel<BeanProfSms> dualList) {
        this.dualList = dualList;
    }

    
    public void handleFileUpload(FileUploadEvent event) {
        try {
            UploadedFile uf = event.getFile();
            Uploader uploader = new Uploader(uf.getInputstream(), uf.getFileName(), selectedUser.getSystemUser() + File.separator + ".attachments" + File.separator + client.getIesClient().getCoreCfg().anyAcademic + File.separator);
            HashMap<String, Object> map = uploader.getResultMap();
            if (!uploader.isSuccess()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, map.get("upload.result") + " " + map.get("clamscan.result"), ""));
                return;
            }
            //Problem if fi is null
            FileInfo fi = (FileInfo) map.get("upload.fileinfo");
            BeanMensajesAttachment beanMensajesAttachment = new BeanMensajesAttachment(client.getSgdClient());
            beanMensajesAttachment.setAttachment(fi.getName());
            beanMensajesAttachment.setSize(fi.getSize());
            getAttachments().add(beanMensajesAttachment);
            //System.out.println("Now attachments is "+getAttachments().size());
        } catch (IOException ex) {
            Logger.getLogger(mbSms.class.getName()).log(Level.SEVERE, null, ex);
        }
       
   }

    public ArrayList<BeanMensajesAttachment> getAttachments() {
        return attachments;
    }

//------------------------------------------------------------   
//    Remove attachment
//------------------------------------------------------------
    public void removeAttachment(BeanMensajesAttachment att)
    {
    
        //Delete the attachment from uploaded location
        File f = new File(fileHostingDir+att.getAttachment());
        f.delete();
    
        //Delete from list
        getAttachments().remove(att);
    }
    

    public void setAttachments(ArrayList<BeanMensajesAttachment> attachments) {
        this.attachments = attachments;
    }
    
    
 private void generateEquipDocent() {
        
        
        equipDocent = new ArrayList<String>();
        
      
//Aquesta select dona caps de departament (codigo tarea=3)
//SELECT DISTINCT p.idProfesores,p.nombre FROM horarios AS h INNER JOIN tareas AS t ON t.idProfesores = h.idTareas 
//INNER JOIN profesores AS p ON p.idProfesores=h.idProfesores WHERE t.codigo=3

//Aquesta select dona tots els tutors (codigo tarea=35)
//SELECT DISTINCT p.idProfesores,p.nombre FROM horarios AS h INNER JOIN tareas AS t ON t.idProfesores = h.idTareas 
//INNER JOIN profesores AS p ON p.idProfesores=h.idProfesores WHERE t.codigo=35
        
    
//Aquesta select dona l'equip docent d'un grup te per tutor idProfesores        
     
String SQL1 = " SELECT DISTINCT p.id FROM "+
        " grupos AS g   "+
        " INNER JOIN  "+
        " grupasig AS ga  "+
        " ON g.id=ga.idGrupos  "+
        " INNER JOIN  "+
        " clasesdetalle AS cd "+ 
        " ON cd.idGrupasig = ga.id "+ 
        " INNER JOIN "+
        " horarios AS h  "+
        " ON h.idClases = cd.idClases "+ 
        " INNER JOIN profesores AS p  "+
        " ON p.id = h.idProfesores  "+
        " WHERE g.idProfesores="+ selectedUser.getIdProfesor() +" ORDER BY p.nombre";
     
        Statement st;
        try {
            st = client.getIesClient().getSgd().createStatement();
            ResultSet rs1 = client.getIesClient().getSgd().getResultSet(SQL1,st);
            while(rs1!=null && rs1.next())
            {
                equipDocent.add(rs1.getString(1));
            }
            if(rs1!=null) {
                rs1.close();
            }
            st.close();
        } catch (SQLException ex) {
            Logger.getLogger(mbSms.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public boolean isSelectEquipDocent() {
        return selectEquipDocent;
    }

    public void setSelectEquipDocent(boolean selectEquipDocent) {
        this.selectEquipDocent = selectEquipDocent;
    }
    
    /**
     * Change equip docent
     */
    public void onToggleEquipDocent()
    {
        
        if(equipDocent==null)
        {
            //Lazy initialization
            this.generateEquipDocent();
        }
        
        ArrayList<BeanProfSms> source = new ArrayList<BeanProfSms>();
        ArrayList<BeanProfSms> target = new ArrayList<BeanProfSms>();
        
        if(selectEquipDocent)
        {
            //if not in target Add it (and remove it from source)
            for(BeanProfSms prof: professors)
            {
                BeanProfSms pointer = null;
                for(String codigo : equipDocent )
                {
                    if(codigo.equals(prof.getCodigo()))
                    {
                        pointer = prof;
                        break;
                    }
                }
                if(pointer!=null)
                {
                    target.add(pointer);                    
                } 
                else
                {
                    source.add(prof);
                }
            }
            
            dualList = new DualListModel<BeanProfSms>(source, target);
        }
        else
        {
            dualList = new DualListModel<BeanProfSms>(professors, target);
        }
    }

    public SelectItem[] getOpcionesListas() {
        return opcionesListas;
    }

    public String getSelectedListas() {
        return selectedListas;
    }

    public void setSelectedListas(String selectedListas) {
        this.selectedListas = selectedListas;
    }

    //Add teachers-------------------------------------------------------------
    public void onSelectLista()
    {
        ArrayList<BeanProfSms> source = new ArrayList<BeanProfSms>();
        ArrayList<BeanProfSms> target = new ArrayList<BeanProfSms>();
      
        if(selectedListas==null)
        {
            return;
        }
        int idLlista = Integer.parseInt(selectedListas);
        System.out.println("idLlista -> "+idLlista);
        if(idLlista>0) //he triat una llista
        {
            MensajesListas pointer = null;
            for(MensajesListas ml: mensajesListas)
            {
                if(ml.getId()==idLlista)
                {
                    pointer = ml;
                    break;
                }
            }
            System.out.println("pointer is "+pointer);
            
            if(pointer!=null)
            {
                System.out.println(pointer.getNombre()+" llista triada...");
                ArrayList<MensajesListasProfesores> listMensajesListasProfesores = pointer.getListMensajesListasProfesores();
                for(BeanProfSms prof: professors )
                {
                    String codigo = prof.getCodigo();

                    BeanProfSms p = null;
                    for (MensajesListasProfesores mlp: listMensajesListasProfesores) {
                        if (mlp.getCodigo().equals(codigo)) {
                            p = prof;
                            break;
                        }
                    }
                    if (p != null) {
                        target.add(p);
                    }
                    else
                    {
                        source.add(prof);
                    }

                }
                dualList = new DualListModel<BeanProfSms>(source, target);
            }
            else
            {
                dualList = new DualListModel<BeanProfSms>(professors, new ArrayList<BeanProfSms>());
            }
            
        }
    }
    
    
    public void onOutboxRowToggle(BeanOutbox sms)
    {
        if(sms!=null && sms.getRichText()==null)
        {
            String loadRichText = client.getSgdClient().getMensajesCollection().loadRichText(sms.getTexto(), sms.getId());
            sms.setRichText(loadRichText);
        }
    }
}