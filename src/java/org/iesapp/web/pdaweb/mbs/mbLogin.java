/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

import org.iesapp.core.util.Client;
import org.iesapp.core.util.CoreCfg;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.iesapp.clients.iesdigital.guardies.Presencia;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.clients.sgd7.profesores.Profesores;
import org.iesapp.util.DataCtrl;
import org.primefaces.context.RequestContext;
import org.iesapp.web.cloudws.FacesUtil;
import org.iesapp.web.pdaweb.util.MobileDeviceDetector;

/**
 *
 * @author Josep
 */
@Named(value = "mbLogin")
//ManagedBean(name="mbLogin")
@SessionScoped
public class mbLogin implements Serializable {
 
    private String version;
    private String build;
    //private String username;
    private String systemUser;
    private String password;
    private String password2;
    private BeanProfesor selectedUser;
    private final HashMap<String, Integer> numIntents;
    private String tipusPDA = "1";
    private boolean tipusPDABool = false;
    private int solPendents;
    private int newmail;
    protected Preferences preferences;
    private java.util.Date avui;
    private String diesFestius;
    private final String type;
    protected String agent;
    protected boolean agentAlert=false;
    private Client client;

    public mbLogin() //Default constructor
    {
        //Inicia un nou client -> Container of coreCfg and both clients
        client = new Client();
        FacesUtil.setSessionMapValue("client", client);
        
        numIntents = new HashMap<String, Integer>();
        preferences = new Preferences();
        avui = new java.util.Date();
        
        //Això dona un problema si es null
        diesFestius = client.getIesClient().getCoreCfg().getFestiusJSON().toString().replaceAll("=",":"); //JSON for calendar

        type = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("type");
        
        if(type!=null && type.startsWith("g"))
        {
            tipusPDA="2";
        }
        String user = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("user");
        
        if(user!=null)
        {
            systemUser = user;
        }
        
        final ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        final HttpServletRequest request =(HttpServletRequest) ec.getRequest();
        agent = request.getHeader("user-agent");
        MobileDeviceDetector detector = new MobileDeviceDetector();
        try {
            if(detector.isMobile(request))
            {
                ec.redirect(request.getContextPath().replace("pdaweb", "pdamobile"));
            }
            
        } catch (Exception ex) {
            Logger.getLogger(mbLogin.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        //System.out.println(userAgent);
        agentAlert = !agent.contains("Firefox") && !agent.contains("Chrome");
        if(agentAlert)
        {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,"Atenció",
                "Us recomanem que utilitzeu Firefox 6.0+, Chrome o Safari 5.0+"));
        }
    }

    public void logout() {
        if(client!=null)
        {
            client.getIesClient().getCoreCfg().outStamp();
        }   
        systemUser = "";
        password = "";
        preferences.reset();

        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        // Deixa anar les connexions
        client.close();
        
        if(type==null) {
            FacesUtil.sendRedirect("index.jsp");
        }
        else
        {
            FacesUtil.sendRedirect("login.jsf?type="+type);
        }
        ((HttpSession) externalContext.getSession(false)).invalidate();

    }

    public String switchPDA(int tipus) {

        preferences.setTipusPDA(tipus);

        String desti = null;
        if (tipus == Preferences.GUARDIAPDA) {
            desti = "substitucions";
            FacesUtil.sendRedirect("substitucions.jsf");
        } else if (tipus == Preferences.PERSONALPDA) {
            desti = preferences.getPaginaInicia();
        }

        return desti;

    }

    public String login() {

        String path = "";
        
        String pwdSU = (String) CoreCfg.configTableMap.get("pdawebSU");
        if(pwdSU==null) {
            pwdSU = (String) CoreCfg.configTableMap.get("adminPwd");
        }
         
        FacesContext context = FacesContext.getCurrentInstance();
        //retrive form values
        boolean mysqlCon = client.getIesClient().getMysql()!=null && !client.getIesClient().getMysql().isClosed(); 
        boolean sgdCon = client.getIesClient().getSgd()!=null && !client.getIesClient().getSgd().isClosed(); 
        
        if (!sgdCon || !mysqlCon) {
            String quines = "";
            if (!sgdCon) {
                quines += "sgd;";
            }
            if (!mysqlCon) {
                quines += "fitxes;";
            }
            context.addMessage("username", new FacesMessage(FacesMessage.SEVERITY_FATAL, "Fatal: ", "No hi ha connexió amb les\nbases de dades del servidor\n" + quines));
            return path + "login";
        }

        Profesores prof = client.getSgdClient().getProfesores();

        //Filtra valors incorrectes del username
//        int upid = -1;
//        try {
//            upid = Integer.parseInt(username);
//        } catch (java.lang.NumberFormatException ex) {
//            username = ""; //invalid user id          
//        }

        if (systemUser.isEmpty()) {
            return path + "login";
        }

        
        prof.loadByWinUser(systemUser); //aqui systemUser es el nom d'usuari de windows
       
        selectedUser = prof.getBean();
        

        //.println("Professor "+selectedUser.getAbrev()+" es tutor ? "+selectedUser.isTutor());

        //Determina si el professor existeix o no (si l'ha trobat, la seva id es valida)
        boolean userexists = !prof.getIdProfesor().isEmpty();
        if (!userexists) {
            context.addMessage("username", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Atenció: ", "Usuari desconegut"));
            return path + "login";
        }

        if (selectedUser.isBloqueoMyClass()) {
            if (getPassword2() != null && getPassword2().equals(pwdSU)) {
                numIntents.remove(selectedUser.getIdProfesor());
                prof.unlock();
            } else {
                context.addMessage("username", new FacesMessage(FacesMessage.SEVERITY_WARN, "Atenció: ", "Aquest compte està bloquejat,\nnecessita la clau d'administració\nper desbloquejar-lo"));
                return path + "login";
            }
        }

        /**
         * Prepara el login:
         */
        if (userexists && (prof.getClaveUP().equals(password) || password.equals(pwdSU)) ) {

            //VALID LOGIN
            //Enable logger as set current user in clients
            //System.out.println(client.getIesClient().getCoreCfg().configTableMap);
            boolean enablelogger = CoreCfg.configTableMap.get("sgdEnableLogger")==null? false:(Boolean) CoreCfg.configTableMap.get("sgdEnableLogger");
            client.getSgdClient().setUser(prof, enablelogger);
           
            prof.getAbrev(client.getIesClient().getMysql());
            client.getIesClient().getCoreCfg().inStamp(prof.getAbrev());
            //We must pass CurrentUser to ies-client 
            client.getIesClient().setIuser(prof.getAbrev());
         
            newmail = client.getSgdClient().getMensajesCollection().getSmsNoLeidos(prof.getIdProfesor());
            FacesUtil.setSessionMapValue("newMail", newmail);
            //System.out.println("newmail="+newmail);

            prof.loadAbrev(client.getIesClient().getMysql()); //determina el codig de la base de fitxes
            preferences.loadPreferences(client.getIesClient().getMysql(), prof.getAbrev());

            selectedUser.setAbrev(prof.getAbrev());
            FacesUtil.setSessionMapValue("selectedUser", selectedUser);

            //determina si ha signat en la base de fitxes
            Presencia pres = new Presencia(client.getIesClient());
            boolean haSignat = pres.haSignat(prof.getAbrev());
            if (!haSignat) {
                pres.autoSigna(prof.getAbrev());
                context.addMessage("username", new FacesMessage(FacesMessage.SEVERITY_INFO, "Informació: ", "S'han signat algunes hores"));
           
            }


            /**
             * Si hi ha sol·licituds pendents mostra un dialeg
             */
            int npend = client.getIesClient().getMissatgeriaCollection().getNumSolPendents();
            setSolPendents(npend);

            if (tipusPDA.equals("1")) {
                preferences.setTipusPDA(Preferences.PERSONALPDA);
                if (newmail == 0) {
                    if (solPendents == 0) {
                        return path + preferences.getPaginaInicia();
                    } else {
                        return path + "missatgeria";
                    }
                } else {
                    return path + "missatges";
                }
            } else if (tipusPDA.equals("2")) {
                preferences.setTipusPDA(Preferences.GUARDIAPDA);
                return path + "substitucions";
            } else if (tipusPDA.equals("3")) {
                return null; //not yet implemented
            }



        } else if (userexists) {

            String codig = selectedUser.getIdProfesor();
            int num = 0;
            if (numIntents.containsKey(codig)) {
                num = numIntents.get(codig) + 1;
                numIntents.put(codig, num);
                if (num > 1) {
                    prof.lock();
                }
            } else {
                numIntents.put(codig, num + 1);
                num = 1;
            }
            context.addMessage("username", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Atenció: ", "Usuari i/o contrasenya invàlides. Resten " + (3 - num) + " intents."));
            return path + "login";
        }

        return null;

    }

    public void newTab(String url) {
        RequestContext context = RequestContext.getCurrentInstance();
        context.execute("window.open('" + url + "', '_newtab')");
    }

    public void onSlideEnd(org.primefaces.event.SlideEndEvent ev) {
        preferences.setPp_fontscale(ev.getValue());
    }

    public void savePreferences() {
        //System.out.println("saving preferences....");
        preferences.save();
    }

    public void restauraPreferences() {
        preferences.reset();
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
     * @return the password2
     */
    public String getPassword2() {
        return password2;
    }

    /**
     * @param password2 the password2 to set
     */
    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String date2str(java.util.Date date) {
        String str = date == null ? "" : new DataCtrl(date).getDiaMesComplet();
        return str;
    }

    /**
     * @return the tipusPDA
     */
    public String getTipusPDA() {
        return tipusPDA;
    }

    /**
     * @param tipusPDA the tipusPDA to set
     */
    public void setTipusPDA(String tipusPDA) {
        this.tipusPDA = tipusPDA;
    }

    /**
     * @return the tipusPDABool
     */
    public boolean isTipusPDABool() {
        return tipusPDABool;
    }

    /**
     * @param tipusPDABool the tipusPDABool to set
     */
    public void setTipusPDABool(boolean tipusPDABool) {
        this.tipusPDABool = tipusPDABool;
    }

    /**
     * @return the solPendents
     */
    public int getSolPendents() {
        return solPendents;
    }

    /**
     * @param solPendents the solPendents to set
     */
    public void setSolPendents(int solPendents) {
        this.solPendents = solPendents;
    }

    /**
     * @return the newmail
     */
    public int getNewmail() {
        return newmail;
    }

    /**
     * @param newmail the newmail to set
     */
    public void setNewmail(int newmail) {
        this.newmail = newmail;
    }

    /**
     * @return the preferences
     */
    public Preferences getPreferences() {
        return preferences;
    }

    /**
     * @param preferences the preferences to set
     */
    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public java.util.Date getAvui() {
        return avui;
    }

    public String getDiesFestius() {
        return diesFestius;
    }

    /**
     * @return the systemUser
     */
    public String getSystemUser() {
        return systemUser;
    }

    /**
     * @param systemUser the systemUser to set
     */
    public void setSystemUser(String systemUser) {
        this.systemUser = systemUser;
    }

    /**
     * @return the agent
     */
    public String getAgent() {
        return agent;
    }

    /**
     * @return the agentAlert
     */
    public boolean isAgentAlert() {
        return agentAlert;
    }

    public String getVersion() {
        return CoreCfg.VERSION;
    }

    public String getBuild() {
        return CoreCfg.BUILD;
    }
   
}
