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
import org.iesapp.fitxes.forms.BeanFitxaCursSimple;
import org.iesapp.fitxes.forms.FitxaCursSimple;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.iesapp.clients.iesdigital.dates.DatesControl;
import org.iesapp.clients.iesdigital.fitxes.BeanDadesPersonals;
import org.iesapp.clients.sgd7.alumnos.BeanAlumno;
import org.iesapp.clients.sgd7.base.BeanTipoObservaciones;
import org.iesapp.clients.sgd7.clases.BeanClase;
import org.iesapp.clients.sgd7.incidencias.BeanIncidencias;
import org.iesapp.clients.sgd7.incidencias.Incidencias;
import org.iesapp.clients.sgd7.profesores.BeanProfesor;
import org.iesapp.util.DataCtrl;
import org.iesapp.util.StringUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.iesapp.web.pdaweb.beans.StudentStat;
import org.iesapp.web.pdaweb.data.AlumnoDataModel;
import org.iesapp.web.pdaweb.data.Data;
import org.iesapp.web.cloudws.FacesUtil;

@Named(value = "mbLlista")
//@ManagedBean(name="mbLlista")
@SessionScoped
//@ViewScoped
public class mbLlista implements Serializable{

    private BeanProfesor selectedUser;
    private String selectedHora = "";
    private ArrayList<BeanClase> horari;
    private SelectItem[] opciones;
    private java.util.Date dia;
    private AlumnoDataModel modelLlistaClasse;
    private ArrayList<BeanAlumno> llistaClasse;
    private BeanAlumno alumnoSelected;
    private ArrayList<BeanIncidencias> llistaInc;
    private BeanIncidencias selectedInc;
    private String selectedIncTooltip;
    private HashMap<String, Integer> mapaInc2Id;
    //dialog
    private String selectedMenu = "0";
    private SelectItem[] opcionesMotius;
    private String comentaris = "";
    private int currentIncId;
    private java.util.Date avui;
    private BeanDadesPersonals dadesPersonals;
    private ArrayList<BeanFitxaCursSimple> cursosIES;
    private int solPendents;
    private String diastr;
    private String dayAlert="";
    private String seguimiento="";
    private StudentStat stat;
    private boolean southCollapsed=true;
    private final Client client;

    public mbLlista() //Default constructor
    {
        
        client = (Client) FacesUtil.getSessionMapValue("client");
        client.getIesClient().getCoreCfg().addAction("enter-llista");
        selectedUser = (BeanProfesor) FacesUtil.getSessionMapValue("selectedUser");

        Calendar cal = Calendar.getInstance();
         cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        avui = cal.getTime();
        dia = avui;
        diastr = new DataCtrl(dia).getDataSQL();

        mapaInc2Id = client.getSgdClient().getTipoIncidencias().getMapaInc2Id();

        ArrayList<BeanTipoObservaciones> tipoObservaciones = client.getSgdClient().getTipoObservaciones().getTipoObservaciones();

        int nn = tipoObservaciones.size();

        opcionesMotius = new SelectItem[nn + 1];
        opcionesMotius[0] = new SelectItem("0", "Tria una opció");

        for (int i = 1; i < nn + 1; i++) {
            opcionesMotius[i] = new SelectItem(tipoObservaciones.get(i - 1).getId(), tipoObservaciones.get(i - 1).getNombre());
        }

        loadHorari();
        loadTable();      
         
        dadesPersonals = new BeanDadesPersonals(client.getIesClient());
    }
    
    public String loadTable() {

        seguimiento = "";
        //borra la taula d'incidencies
        llistaInc = new ArrayList<BeanIncidencias>();
        llistaClasse = new ArrayList<BeanAlumno>();
        alumnoSelected = null;

        //Check selectedHora
        if (selectedHora == null || selectedHora.isEmpty()) {
            selectedHora="";
            return null;
        }

        int idOldAlumno = -1;
        if (alumnoSelected != null) {
            idOldAlumno = alumnoSelected.getOrdre() - 1;
        }


        int ordre = (int) Double.parseDouble(getSelectedHora());
        if (ordre >= 0) {
            BeanClase bh = getHorari().get(ordre);
            seguimiento = bh.getSeguimiento();
             //Hi ha una anotacio ! maximitza el seguiment
            
            RequestContext context = RequestContext.getCurrentInstance();
            if(context!=null)
            {
                    if( (seguimiento!=null && !seguimiento.isEmpty() && southCollapsed))
                    {
                        southCollapsed = false;
                        context.execute("myLayout.toggle('south');");               
                    }                    
                    else if( (seguimiento==null || seguimiento.isEmpty()) && !southCollapsed)
                    {   
                        southCollapsed = true;
                        context.execute("myLayout.toggle('south');");               
                    }                    
            }
            
            int horaCentro = getHorari().get(ordre).getIdHorasCentro();
            //This is to pass to actividades
            FacesUtil.setSessionMapValue("selectedBeanClase", bh);

            //Comprova si la clase esta anotada
            //Nomes ho comprova si dia es avui
            //System.out.println(dia + ", " + avui);
            if (dia.equals(avui)) {
                ArrayList<Integer> isanotada = client.getSgdClient().getClasesAnotadasCollection().isAnotada(selectedUser.getIdProfesor(),
                        new java.sql.Date(dia.getTime()), horaCentro, bh.getIdGrupAsigs());
                //System.out.println("isanotada returns" + isanotada.toString());
                if (isanotada.isEmpty()) {
                    client.getSgdClient().getClasesAnotadasCollection().anotaClase(selectedUser.getIdProfesor(), selectedUser.getIdProfesor(),
                            horaCentro, bh.getIdGrupAsigs());
                }
            }

            llistaClasse.clear();
           
            llistaClasse = client.getSgdClient().getAlumnosCollection().getAlumnosProfeClase(selectedUser.getIdProfesor(), bh.getIdClase());


            for (int i = 0; i < llistaClasse.size(); i++) {
                llistaClasse.get(i).setOrdre(i + 1);
             
                //determina si l'alumne esta expulsat
                int code=getSancio(llistaClasse.get(i).getExpediente());
                if(code!=0)
                {
                    llistaClasse.get(i).setStatus(code);
                }
                
                //Carrega les incidencies de cada alumne
                int idAlumno = llistaClasse.get(i).getId();
                int idGrupAsig = llistaClasse.get(i).getIdGrupoAsig();
                ArrayList<BeanIncidencias> inc_local = client.getSgdClient().getIncidenciasCollection().getIncidencias(idAlumno, horaCentro, idGrupAsig, selectedUser.getIdProfesor(), new DataCtrl(dia).getDataSQL());
                String inc_local_txt = "";
                for (int k = 0; k < inc_local.size(); k++) {
                    inc_local_txt += inc_local.get(k).getSimbolo() + "; ";
                }
                llistaClasse.get(i).setIncidencies(inc_local_txt);

                //System.out.println("llistaClasse " + llistaClasse.get(i).getOrdre() + " " + llistaClasse.get(i).getNombre() + llistaClasse.get(i).getIncidencies());
            }
            if (!llistaClasse.isEmpty()) {
             
                alumnoSelected = llistaClasse.get(0);               
                loadIncidenciesAlumne();
                //System.out.println("selected " + alumnoSelected.getNombre());
            }


        }

        return null;
    }

   
     private int getSancio(int expediente) {
        String SQL1= "SELECT * FROM tuta_dies_sancions WHERE exp2="+expediente
                + " AND desde<='"+diastr+"' AND '"+diastr+"'<=fins";
        int status =0;
        
        try {
            Statement st = client.getIesClient().getMysql().createStatement();
            ResultSet rs2 = client.getIesClient().getMysql().getResultSet2(SQL1,st);
            if(rs2!=null && rs2.next())
            {
                String tipus = rs2.getString("tipus");
                if(tipus.equalsIgnoreCase("EXPULSIO")) {
                    status = 1;
                }
                else if(tipus.equalsIgnoreCase("DIMECRES")) {
                    status = 2;
                }
            }
            if(rs2!=null) {
                rs2.close();
                st.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(mbLlista.class.getName()).log(Level.SEVERE, null, ex);
        }
        return status;
    }
     
    public void loadDadesPersonals() {
        if(alumnoSelected==null) {
            return;
        }
        dadesPersonals = new BeanDadesPersonals(client.getIesClient());
        dadesPersonals.getFromDB(alumnoSelected.getExpediente(), client.getIesClient().getCoreCfg().anyAcademic);
        setCursosIES(new FitxaCursSimple(client.getIesClient(), alumnoSelected.getExpediente()).getInfoAnys());
    }

    public void carregaHorari(SelectEvent event) {
        dia = (java.util.Date) event.getObject();
        diastr = new DataCtrl(dia).getDataSQL();
        loadHorari();
    }

    public void goToday()
    {
        dia = avui;
        diastr = new DataCtrl(dia).getDataSQL();
        loadHorari();
    }
    
    public String loadHorari() {
        selectedHora = "";
        Calendar cal = Calendar.getInstance();
        cal.setTime(dia);
        int diaSetmana = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (diaSetmana <= 0) {
            diaSetmana = 7;
        }
        if (selectedUser == null) {
            return "login";
        }

        dayAlert = "";
        boolean esfestiu = new DatesControl(dia, client.getIesClient()).esFestiu();
        if(!dia.equals(avui))
        {
            dayAlert = "Tornau a avui";
        }
        if(esfestiu)
        {
            dayAlert = "Dia no lectiu";
        }
        
        //Si es festiu desabilitat tot per impedir que el
        //professorat introdueixi incidencies...
         if(esfestiu) 
         {
            selectedHora="";
            horari = new ArrayList<BeanClase>();
            opciones = new SelectItem[getHorari().size()];
            //borra la taula d'incidencies
            llistaInc = new ArrayList<BeanIncidencias>();
           
            llistaClasse = new ArrayList<BeanAlumno>();
            alumnoSelected = null;
            return null;
         }
         horari = client.getSgdClient().getClases(selectedUser.getIdProfesor(), diaSetmana).getHorario();
         
         //Carrega les anotacions d'aquestes clases (a dia triat)
         for(int i=0; i<horari.size(); i++)
         {
             String seguimiento = client.getSgdClient().getAnotacionesClase().getAnotacionesClase(horari.get(i).getIdClase(), selectedUser.getIdProfesor(), horari.get(i).getIdHorasCentro(), dia);
             horari.get(i).setSeguimiento(seguimiento);                    
         }
     
        
        opciones = new SelectItem[getHorari().size()];

        Iterator it = getHorari().iterator();
        int i = 0;

        Calendar cal_now = Calendar.getInstance();

        while (it.hasNext()) {
            BeanClase bh = (BeanClase) it.next();
            opciones[i] = new SelectItem(i + "",
                    "[" + StringUtils.formatTime(bh.getInicio()) + "-"
                    + StringUtils.formatTime(bh.getFin()) + "] "
                    + bh.getGrupo() + " · "
                    + bh.getMateria() + " · " + bh.getAula());

            Calendar cal_clases_ini = Calendar.getInstance();
            Calendar cal_clases_fin = Calendar.getInstance();

            cal_clases_ini.setTime(bh.getInicio());
            cal_clases_fin.setTime(bh.getFin());

            cal_clases_ini.set(Calendar.DAY_OF_MONTH, cal_now.get(Calendar.DAY_OF_MONTH));
            cal_clases_fin.set(Calendar.DAY_OF_MONTH, cal_now.get(Calendar.DAY_OF_MONTH));

            cal_clases_ini.set(Calendar.YEAR, cal_now.get(Calendar.YEAR));
            cal_clases_fin.set(Calendar.YEAR, cal_now.get(Calendar.YEAR));

            cal_clases_ini.set(Calendar.MONTH, cal_now.get(Calendar.MONTH));
            cal_clases_fin.set(Calendar.MONTH, cal_now.get(Calendar.MONTH));

            if (cal_clases_ini.before(cal_now) && cal_now.before(cal_clases_fin)) {
                setSelectedHora("" + i);
            }

            i += 1;
        }
        if (getHorari().size() > 0) {

            BeanClase bh = getHorari().get(i - 1);
            Calendar cal_clases_ini = Calendar.getInstance();
            Calendar cal_clases_fin = Calendar.getInstance();

            cal_clases_ini.setTime(bh.getInicio());
            cal_clases_fin.setTime(bh.getFin());

            cal_clases_ini.set(Calendar.DAY_OF_MONTH, cal_now.get(Calendar.DAY_OF_MONTH));
            cal_clases_fin.set(Calendar.DAY_OF_MONTH, cal_now.get(Calendar.DAY_OF_MONTH));

            cal_clases_ini.set(Calendar.YEAR, cal_now.get(Calendar.YEAR));
            cal_clases_fin.set(Calendar.YEAR, cal_now.get(Calendar.YEAR));

            cal_clases_ini.set(Calendar.MONTH, cal_now.get(Calendar.MONTH));
            cal_clases_fin.set(Calendar.MONTH, cal_now.get(Calendar.MONTH));

            if (cal_now.after(cal_clases_fin)) {
                setSelectedHora("" + (i - 1));
            }

            if (selectedHora.isEmpty()) {
                selectedHora = "";
            }
        }
        loadTable();

        return null;

    }
    
    public void moveUp()
    {
        if(alumnoSelected!=null && alumnoSelected.getOrdre()>1)
        {
            int ordre = alumnoSelected.getOrdre()-1;
            alumnoSelected = this.llistaClasse.get(ordre-1);
            this.loadIncidenciesAlumne();
        }
    }
    
    public void moveDown()
    {
      
        if(alumnoSelected!=null && alumnoSelected.getOrdre()<this.llistaClasse.size())
        {
            int ordre = alumnoSelected.getOrdre()-1;
            alumnoSelected = this.llistaClasse.get(ordre+1);
            this.loadIncidenciesAlumne();
        }
        
    }

    public SelectItem[] getOpciones() {
        return opciones;
    }

    public String getSelectedHora() {
        return selectedHora;
    }

    public void setSelectedHora(String hora) {
        this.selectedHora = hora;
    }

    /**
     * @return the dia
     */
    public java.util.Date getDia() {
        return dia;
    }

    /**
     * @param dia the dia to set
     */
    public void setDia(java.util.Date dia) {
        this.dia = dia;
    }

    /**
     * @param llistaClasse the llistaClasse to set
     */
    public void setLlistaClasse(ArrayList<BeanAlumno> llistaClasse) {
        this.llistaClasse = llistaClasse;
    }

    public void onSelectAlumne() {
        //System.out.println("listener to row changes");
    }

    /**
     * @return the alumnoSelected
     */
    public BeanAlumno getAlumnoSelected() {
        return alumnoSelected;
    }

    /**
     * @param alumnoSelected the alumnoSelected to set
     */
    public void setAlumnoSelected(BeanAlumno alumnoSelected) {
        this.alumnoSelected = alumnoSelected;
    }

    public void getIncidenciesAlumne(SelectEvent event) {
        alumnoSelected = (BeanAlumno) event.getObject();
        //alumnoSelected = (BeanAlumno) modelLlistaClasse.getRowData();
        //prova :: torn a generar el model de la taula
        //modelLlistaClasse = new AlumnoDataModel(llistaClasse);
        //

        loadIncidenciesAlumne();
    }

    private void loadIncidenciesAlumne() {

        //Fa un reset dels comentaris
        comentaris = "";
        selectedMenu = "0";

        int id = (int) Double.parseDouble(getSelectedHora());
        int horaCentro = getHorari().get(id).getIdHorasCentro();

        int idAlumno = alumnoSelected.getId();
        int idGrupAsig = alumnoSelected.getIdGrupoAsig();
        ////System.out.println("try to undertand images "+alumnoSelected.getPhoto());
        llistaInc.clear();
      
        llistaInc = client.getSgdClient().getIncidenciasCollection().getIncidencias(idAlumno, horaCentro, idGrupAsig, selectedUser.getIdProfesor(), new DataCtrl(dia).getDataSQL());
        
        if (!llistaInc.isEmpty()) {
            selectedInc = llistaInc.get(0);
        }
    }

    public void onSelectedIncidencia(org.primefaces.event.SelectEvent event) {
          
        selectedInc = (BeanIncidencias) event.getObject();
        //System.out.println("Estic carregant la incidencia " + selectedInc.getId() + " " + selectedInc.getSimbolo() + " " + selectedInc.getIdTipoObservaciones() + " "+ selectedInc.getObservaciones());

        //Fa un reset dels comentaris
        comentaris = selectedInc.getObservaciones();
        selectedMenu = "" + selectedInc.getIdTipoObservaciones();

        if (selectedInc != null) {
            selectedIncTooltip = selectedInc.getDescripcion();
        }
        //System.out.println("estic onSelectIncidencia processant el tooltip " + selectedIncTooltip + "; id= " + selectedMenu);
    }

    public void deleteInc() {
        //he d'agafar la selectedInc
        if (selectedInc == null) {
            return;
        }

        int id = selectedInc.getId();
        Incidencias inc = client.getSgdClient().getIncidencias();
        inc.setId(id);
        int removeInc = inc.delete();

        FacesContext context = FacesContext.getCurrentInstance();
        if (removeInc > 0) {
            context.addMessage(null, new FacesMessage("Informació", "S'ha esborrat la incidència " + id));
        } else {
            context.addMessage(null, new FacesMessage("Error", "Impossible esborrar la incidència " + id));
        }

        //loadTable();
        loadIncidenciesAlumne();
        refreshIncInLlistat();
        client.getIesClient().getCoreCfg().addAction("del-inc");
    }

    /**
     * prepara una nova incidencia en blanc
     * i obri un dialeg, quan s'accepta el
     * dialeg guarda la incidencia.
     * 
     * @param event 
     */
    public void preparanovaInc(ActionEvent event) {
        selectedMenu = "0";
        comentaris = "";

        String idComponent = event.getComponent().getId();
        String tipusIncidencia = "";

        String simbol = "";
        

         if (idComponent.equalsIgnoreCase("butfa")) {
            tipusIncidencia = "Falta a classe";
            simbol = "FA";
        } else if (idComponent.equalsIgnoreCase("butre")) {
            tipusIncidencia = "Retard";
            simbol = "RE";
        } else if (idComponent.equalsIgnoreCase("butag")) {
            tipusIncidencia = "Amonestació greu";
            simbol = (String) CoreCfg.configTableMap.get("simbolAmonGreu");           
        } else if (idComponent.equalsIgnoreCase("butal")) {
            tipusIncidencia = "Amonestació lleu";
            simbol = (String) CoreCfg.configTableMap.get("simbolAmonLleu");
        } else if (idComponent.equalsIgnoreCase("butcp")) {
            simbol = "CP";
            tipusIncidencia = "Comentari positiu";
        } else if (idComponent.equalsIgnoreCase("butcn")) {
            simbol = "CN";
            tipusIncidencia = "Comentari negatiu";
        } else if (idComponent.equalsIgnoreCase("butdi")) {
            simbol = (String) CoreCfg.configTableMap.get("simbolCastigDimecres");
            tipusIncidencia = "Càstig de Dimecres";
        }
        int idinc = getSafeIncId(simbol);
        if(idinc<=0)
        {
             FacesContext context = FacesContext.getCurrentInstance();
             context.addMessage(null, new FacesMessage("Error", "No es troba Id de la incidencia simbol=" + simbol));
             return;
        }
      
        currentIncId = idinc;
        int id = (int) Double.parseDouble(getSelectedHora());
        int horaCentro = getHorari().get(id).getIdHorasCentro();

        int idGrupAsig = alumnoSelected.getIdGrupoAsig();
        String idProfe = selectedUser.getIdProfesor();  //figura la id del profe de guardia

        int idmotiu = (int) Double.parseDouble(selectedMenu);

        int idAlumno = alumnoSelected.getId();

        selectedInc = new BeanIncidencias();

        selectedInc.setIdAlumnos(idAlumno);
        selectedInc.setIdProfesores(idProfe);
        selectedInc.setIdTipoIncidencias(idinc);
        selectedInc.setIdHorasCentro(horaCentro);
        selectedInc.setIdGrupAsig(idGrupAsig);
        selectedInc.setDia(new java.sql.Date(dia.getTime()));
        selectedInc.setSimbolo(simbol);
        selectedInc.setDescripcion(tipusIncidencia);

        //Obri la finestra ja que tot ha anat OK
        RequestContext.getCurrentInstance().execute("novaincdlgwv.show()");
    }

    /**
     *  Nomes per les incidencies FA i RE, les quals no demana detalls
     *  Nomes permet una FA o un RE per clase/alumne
     * @param event 
     */
    public void novaInc(ActionEvent event) {
        client.getIesClient().getCoreCfg().addAction("new-inc");
        int idAlumno = alumnoSelected.getId();
        if (idAlumno <= 0) {
            return;
        }

        String idComponent = event.getComponent().getId();
        String tipusIncidencia = "";
        int idinc = -1;
        String simbolo = "";
        if (idComponent.equalsIgnoreCase("butfa")) {
            tipusIncidencia = "Falta a classe";
            simbolo = "FA";
            idinc = mapaInc2Id.get(simbolo);
        } else if (idComponent.equalsIgnoreCase("butre")) {
            tipusIncidencia = "Retard";
            simbolo = "RE";
            idinc = mapaInc2Id.get(simbolo);
        } else if (idComponent.equalsIgnoreCase("butag")) {
            tipusIncidencia = "Amonestació greu";
            simbolo = "AG";
            idinc = mapaInc2Id.get(simbolo);
        } else if (idComponent.equalsIgnoreCase("butal")) {
            tipusIncidencia = "Amonestació lleu";
            simbolo = "AL";
            idinc = mapaInc2Id.get(simbolo);
        } else if (idComponent.equalsIgnoreCase("butcp")) {
            tipusIncidencia = "Comentari positiu";
            simbolo = "CP";
            idinc = mapaInc2Id.get(simbolo);
        } else if (idComponent.equalsIgnoreCase("butcn")) {
            tipusIncidencia = "Comentari negatiu";
            simbolo = "CN";
            idinc = mapaInc2Id.get(simbolo);
        } else if (idComponent.equalsIgnoreCase("butdi")) {
            tipusIncidencia = "Càstig de Dimecres";
            simbolo = "DI";
            idinc = mapaInc2Id.get(simbolo);
        }

        //Comprova si hi ha duplicats
        int ordreF = -1;
        int ordreR = -1;
        for (int i = 0; i < llistaInc.size(); i++) {
            BeanIncidencias inc = llistaInc.get(i);
            if (inc.getSimbolo().equalsIgnoreCase("FA") || inc.getSimbolo().equalsIgnoreCase("F")) {
                ordreF = i;
                break;
            }
            if (inc.getSimbolo().equalsIgnoreCase("RE") || inc.getSimbolo().equalsIgnoreCase("R")) {
                ordreR = i;
                break;
            }
        }

        int success = 0;
        if (simbolo.equals("FA") && ordreR >= 0) //s'ha trobat un retard
        {
            //Canvia el retard existent a falta
            Incidencias incidencia = client.getSgdClient().getIncidencias(llistaInc.get(ordreR));
            incidencia.setIdTipoIncidencias(idinc);
            success = incidencia.save();
        } else if (simbolo.equals("RE") && ordreF >= 0) //s'ha trobat una falta
        {
            //Canvia la falta existent a retard
            Incidencias incidencia = client.getSgdClient().getIncidencias(llistaInc.get(ordreF));
            incidencia.setIdTipoIncidencias(idinc);
            success = incidencia.save();
        } else if ((simbolo.equals("FA") && ordreF < 0) || (simbolo.equals("RE") && ordreR < 0)) {
            //Demano falta i no hi ha falta, demano retard i no hi ha retard (les creo de zero)
            int id = (int) Double.parseDouble(getSelectedHora());
            int horaCentro = getHorari().get(id).getIdHorasCentro();

            int idGrupAsig = alumnoSelected.getIdGrupoAsig();
            String idProfe = selectedUser.getIdProfesor();

            int idmotiu = (int) Double.parseDouble(selectedMenu);

            //System.out.println("Estic introduint nova incidencia...sgd=" + sgd + " : idAlumno=" + idAlumno + " :selectedUser=" + idProfe + " : idInc=" + idinc + ": horaCentro=" + horaCentro + " : idGrupAsig=" + idGrupAsig);
            //System.out.println("Estic introduint nova incidencia...IDMOTIU=" + idmotiu + " comentaris=" + comentaris);

            Incidencias inc = client.getSgdClient().getIncidencias();
            inc.setIdAlumnos(idAlumno);
            inc.setIdProfesores(idProfe);
            inc.setIdTipoIncidencias(idinc);
            inc.setIdHorasCentro(horaCentro);
            inc.setIdGrupAsig(idGrupAsig);
            inc.setIdTipoObservaciones(idmotiu);
            inc.setComentarios(comentaris);
            inc.setDia(new java.sql.Date(dia.getTime()));
            success = inc.save();
        }

        FacesContext context = FacesContext.getCurrentInstance();

        if (success > 0) {
            //  context.addMessage(null, new FacesMessage("Informació", "S'ha introduit incidència " + tipusIncidencia + " amb id=" + success));
        } else {
            context.addMessage(null, new FacesMessage("Error", "Impossible introduir incidència " + tipusIncidencia));
        }

        selectedMenu = "0";
        comentaris = "";

        //He d'incloure a la taula principal d'alumnes la nova incidència
        loadIncidenciesAlumne();
        refreshIncInLlistat();
        //Per a les incidencies de FA o RE, automaticament vaig a l'alumne seguent si existeix
        int ordre = alumnoSelected.getOrdre();
        if(llistaClasse.size()>ordre)
        {
            alumnoSelected = llistaClasse.get(ordre);
        }
        
        //He d'incloure a la taula principal d'alumnes la nova incidència
        loadIncidenciesAlumne();
        refreshIncInLlistat();
      

        //tria la incidencia selected (la darrera)
        if (!llistaInc.isEmpty()) {
            selectedInc = llistaInc.get(llistaInc.size() - 1);
            //System.out.println("me posat a la inc id=" + selectedInc.getId());
        }

    }

    private void refreshIncInLlistat() {
        int ordre = alumnoSelected.getOrdre() - 1;
        String txt = "";
        if (ordre >= 0) {
            for (int i = 0; i < llistaInc.size(); i++) {
                txt += llistaInc.get(i).getSimbolo() + "; ";
            }

            llistaClasse.get(ordre).setIncidencies(txt);
            modelLlistaClasse = new AlumnoDataModel(llistaClasse);
        }
    }

    public void cancelaDlg(ActionEvent actionEvent) {
        //System.out.println("CANCELLA");
        //selectedMenu="-1";
        //comentaris="";
    }

    public void acceptaDlg(ActionEvent actionEvent) {
        int id = -1;
        if (selectedInc != null) {
            id = selectedInc.getId();
        }
        int idInc = 0;
        try {
            idInc = (int) Double.parseDouble(selectedMenu);
        } finally {
        }

        //Comprova si hi ha comentari o incidencia
        FacesContext context = FacesContext.getCurrentInstance();
        if ((idInc == 0 || selectedMenu.isEmpty()) && comentaris.trim().isEmpty()) {
            context.addMessage("Atenció:", new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Cal motiu o observació."));
            return;
        }


        int success = 0;
        if (id > 0) {
            //System.out.println("UPDATE INC:" + id + ", " + idInc + " " + comentaris);

            Incidencias inc = client.getSgdClient().getIncidencias(selectedInc);
            inc.setIdTipoObservaciones(idInc);
            inc.setComentarios(comentaris);
            //System.out.println(inc.print());
            success = inc.save();
        } else {
            //System.out.println("INSERT INC:" + id + ", " + idInc + " " + comentaris);
            Incidencias inc = client.getSgdClient().getIncidencias(selectedInc);
            inc.setComentarios(comentaris);
            inc.setIdTipoObservaciones(idInc);
            success = inc.save();
            //System.out.println(inc.print());

            //He d'incloure a la taula principal d'alumnes la nova incidència
            loadIncidenciesAlumne();
            refreshIncInLlistat();

            //tria la incidencia selected (la darrera)
            if (!llistaInc.isEmpty()) {
                selectedInc = llistaInc.get(llistaInc.size() - 1);
                //System.out.println("me posat a la inc id=" + selectedInc.getId());
            }
        }


        if (success > 0) {
            context.addMessage(null, new FacesMessage("Informació", "S'ha desat correctament"));
        } else {
            context.addMessage(null, new FacesMessage("Error", "No s'ha pogut desar"));
        }

        loadIncidenciesAlumne();
        selectedMenu = "0";
        comentaris = "";
        currentIncId = 0;

        if (id > 0) {
            RequestContext.getCurrentInstance().execute("detallsdlgwv.hide()");
        } else {
            RequestContext.getCurrentInstance().execute("novaincdlgwv.hide()");
        }

    }

    public void onIncEdit() {
        //System.out.println("he pitjat editar la incidencia " + selectedInc.getId() + " " + selectedInc.getDescripcion() + " " + selectedInc.getObservaciones());
        selectedMenu = selectedInc.getIdTipoObservaciones() + "";
        comentaris = selectedInc.getObservaciones();
    }

    public void preparaEstadistica()
    {
        if(alumnoSelected==null) {
            return;
        }
        stat = new Data(client).getStudentStat(alumnoSelected.getId(), alumnoSelected.getIdGrupoAsig(), selectedUser.getIdProfesor());
    }
    
    public void updateSeguimiento()
    {
         client.getIesClient().getCoreCfg().addAction("update-agenda");
        //System.out.println("i am updating anotacions");
        if(selectedHora.isEmpty()) {
            return;
        }
        int ordre = (int) Double.parseDouble(selectedHora);
        if (ordre >= 0) {
            BeanClase bh = getHorari().get(ordre);  
            client.getSgdClient().getAnotacionesClase().setAnotacionesClase(bh.getIdClase(), selectedUser.getIdProfesor(), bh.getIdHorasCentro(), dia, seguimiento);
        
            //Actualitza tambe l'horari
            horari.get(ordre).setSeguimiento(seguimiento);
        }
    }
    /**
     * @return the llistaInc
     */
    public ArrayList<BeanIncidencias> getLlistaInc() {
        return llistaInc;
    }

    /**
     * @param llistaInc the llistaInc to set
     */
    public void setLlistaInc(ArrayList<BeanIncidencias> llistaInc) {
        this.llistaInc = llistaInc;
    }

    /**
     * @return the selectedInc
     */
    public BeanIncidencias getSelectedInc() {
        return selectedInc;
    }

    /**
     * @param selectedInc the selectedInc to set
     */
    public void setSelectedInc(BeanIncidencias selectedInc) {
        this.selectedInc = selectedInc;
    }

    /**
     * @return the selectedIncTooltip
     */
    public String getSelectedIncTooltip() {
        return selectedIncTooltip;
    }

    /**
     * @param selectedIncTooltip the selectedIncTooltip to set
     */
    public void setSelectedIncTooltip(String selectedIncTooltip) {
        this.selectedIncTooltip = selectedIncTooltip;
    }

    /**
     * @return the modelLlistaClasse
     */
    public AlumnoDataModel getModelLlistaClasse() {
        modelLlistaClasse = new AlumnoDataModel(llistaClasse);
        return modelLlistaClasse;
    }

    /**
     * @param modelLlistaClasse the modelLlistaClasse to set
     */
    public void setModelLlistaClasse(AlumnoDataModel modelLlistaClasse) {
        this.modelLlistaClasse = modelLlistaClasse;
    }

    /**
     * @return the selectedMenu
     */
    public String getSelectedMenu() {
        return selectedMenu;
    }

    /**
     * @param selectedMenu the selectedMenu to set
     */
    public void setSelectedMenu(String selectedMenu) {
        this.selectedMenu = selectedMenu;
    }

    /**
     * @return the opcionesMotius
     */
    public SelectItem[] getOpcionesMotius() {
        return opcionesMotius;
    }

    /**
     * @param opcionesMotius the opcionesMotius to set
     */
    public void setOpcionesMotius(SelectItem[] opcionesMotius) {
        this.opcionesMotius = opcionesMotius;
    }

    /**
     * @return the comentaris
     */
    public String getComentaris() {
        return comentaris;
    }

    /**
     * @param comentaris the comentaris to set
     */
    public void setComentaris(String comentaris) {
        this.comentaris = comentaris;
    }

   
    /**
     * @return the dadesPersonals
     */
    public BeanDadesPersonals getDadesPersonals() {
        return dadesPersonals;
    }

    /**
     * @param dadesPersonals the dadesPersonals to set
     */
    public void setDadesPersonals(BeanDadesPersonals dadesPersonals) {
        this.dadesPersonals = dadesPersonals;
    }

    /**
     * @return the cursosIES
     */
    public ArrayList<BeanFitxaCursSimple> getCursosIES() {
        return cursosIES;
    }

    /**
     * @param cursosIES the cursosIES to set
     */
    public void setCursosIES(ArrayList<BeanFitxaCursSimple> cursosIES) {
        this.cursosIES = cursosIES;
    }

    /**
     * @return the solPendents
     */
    public int getSolPendents() {
        return solPendents;
    }


    public void postProcessXLS(Object document) {

        client.getIesClient().getCoreCfg().addAction("xls-llista");
        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow titleRow = sheet.createRow(0);
        
       
        //Define Styles
        HSSFCellStyle styleTitle = wb.createCellStyle();
        styleTitle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        styleTitle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        styleTitle.setBorderBottom(CellStyle.BORDER_DOUBLE);
        styleTitle.setAlignment(CellStyle.ALIGN_CENTER);
        HSSFFont font = wb.createFont();
        font.setFontHeight(HSSFFont.BOLDWEIGHT_BOLD);
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        styleTitle.setFont(font);        
       
       sheet.addMergedRegion(new CellRangeAddress(
            0, //first row (0-based)
            0, //last row  (0-based)
            0, //first column (0-based)
            2  //last column  (0-based)
            ));
        
        HSSFCell createCell = titleRow.createCell(1); 
        createCell.setCellStyle(styleTitle);
        createCell = titleRow.createCell(2); 
        createCell.setCellStyle(styleTitle);
        createCell = titleRow.createCell(0); 
        int ordre = (int) Double.parseDouble(selectedHora);
        if (ordre >= 0) {
            BeanClase bh = getHorari().get(ordre);
            createCell.setCellValue(new HSSFRichTextString("Llistat de "+bh.getGrupo()+" · "+bh.getMateria()));
            createCell.setCellStyle(styleTitle);
        }
        
        //Itera sobre totes les files disponibles
        int indx = 0;
        for(Row row: sheet)
        {
            if(indx>0)
            {
            //Esborra el possible contingut de la darrera
            row.getCell(2).setCellValue("");
            }
            indx += 1;
        }
     
      

        //Autoajusta
        for(int i=0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }

                
}

    public String getDayAlert() {
        return dayAlert;
    }

    public ArrayList<BeanClase> getHorari() {
        return horari;
    }

    public String getSeguimiento() {
        return seguimiento;
    }

    public void setSeguimiento(String seguimiento) {
        this.seguimiento = seguimiento;
    }

    public StudentStat getStat() {
        return stat;
    }
 

    public boolean isSouthCollapsed() {
        return southCollapsed;
    }

    public void setSouthCollapsed(boolean southCollapsed) {
        this.southCollapsed = southCollapsed;
    }

  private int getSafeIncId(String simbol) {
        int idInc = 0;
        if(simbol==null || !mapaInc2Id.containsKey(simbol))
        {
            return idInc;
        }
        else
        {
            idInc = mapaInc2Id.get(simbol);
        }
        return idInc;
    }
}

