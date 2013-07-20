/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

import java.util.ArrayList;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import org.iesapp.clients.iesdigital.anuncis.AnunciBean;
import org.iesapp.clients.iesdigital.anuncis.AnuncisDefinition;
import org.iesapp.core.util.Client;
import org.iesapp.web.cloudws.FacesUtil;

/**
 *
 * @author Josep
 */
@ManagedBean(name = "mbAnuncis")
@SessionScoped
public class mbAnuncis implements java.io.Serializable {

    protected ArrayList<AnunciBean> listAnuncis;
    protected AnunciBean selectedAnunci;
    protected String search="";
    protected int ordreAnunci = 0;
    protected String category = "-1";
    protected SelectItem[] opciones;
    private final Client client;
    protected String limit = "5";
 

    public mbAnuncis() {
        
        client = (Client) FacesUtil.getSessionMapValue("client");
        refreshAnuncis();
        if(listAnuncis.size()>0)
        {
            selectedAnunci = listAnuncis.get(ordreAnunci);
        }
        
         opciones = new SelectItem[AnuncisDefinition.getMapDefined().size() + 1];
         int i=1;
         opciones[0] = new SelectItem("-1", "Tots els anuncis");
         for(AnuncisDefinition adef : AnuncisDefinition.getMapDefined().values())
         {
             opciones[i] = new SelectItem(adef.getAnuncisTypeId(), adef.getAnuncisTypeName());
             i +=1;
         }
         
    }

//    public void next()
//    {
//        if(getOrdreAnunci()<getListAnuncis().size()-1)
//        {
//            setOrdreAnunci(getOrdreAnunci() + 1);
//        }
//        else
//        {
//            setOrdreAnunci(0);
//        }
//        
//        selectedAnunci = getListAnuncis().get(getOrdreAnunci());
//    }
//    
//    public void back()
//    {
//         
//        if(getOrdreAnunci()>0)
//        {
//            setOrdreAnunci(getOrdreAnunci() - 1);
//        }
//        else
//        {
//            setOrdreAnunci(getListAnuncis().size()-1);
//        }
//        
//        selectedAnunci = getListAnuncis().get(getOrdreAnunci());
//    }
//    
    public final void refreshAnuncis()
    {
        listAnuncis = client.getIesClient().getAnuncisClient().loadAnuncis(Integer.parseInt(category), ordreAnunci, limit, search);
    }
  

    public AnunciBean getSelectedAnunci() {
        return selectedAnunci;
    }

    public void setSelectedAnunci(AnunciBean selectedAnunci) {
        this.selectedAnunci = selectedAnunci;
    }

    public ArrayList<AnunciBean> getListAnuncis() {
        return listAnuncis;
    }

    public void setListAnuncis(ArrayList<AnunciBean> listAnuncis) {
        this.listAnuncis = listAnuncis;
    }

    public int getOrdreAnunci() {
        return ordreAnunci;
    }

    public void setOrdreAnunci(int ordreAnunci) {
        this.ordreAnunci = ordreAnunci;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public SelectItem[] getOpciones() {
        return opciones;
    }

    public void setOpciones(SelectItem[] opciones) {
        this.opciones = opciones;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
    
//      private int getListAnuncis(String mode, int type, int order) {
//        int nv = 0;
//        ordreAnunci = 0;
//        setListAnuncis(new ArrayList<AnunciBean>());
//        String SQL1;
//        java.util.Date avui = new java.util.Date();
//
//        String cond = "";
//        if(type>=0)
//        {
//            cond = " AND mis.missatge LIKE '%type={"+type+"}%' ";
//        }
//        
//        String orderCond = " ORDER by data DESC, id DESC"; //Order by post date
//        if (order == 1) {
//            //Order by first event date
//            orderCond = " ORDER BY STR_TO_DATE(REPLACE(LEFT(RIGHT(missatge,LENGTH(missatge)-10-LOCATE('eventDate={',missatge)),10),'-','.'), GET_FORMAT(DATE, 'EUR')) DESC, id DESC";
//        }
//
//        if(!limit.isEmpty() && !limit.equals("*"))
//        {
//            orderCond += " LIMIT "+limit;
//        }
//        SQL1 = "SELECT mis.id, mis.data, mis.missatge, mis.de as abrev FROM "
//                + " sig_missatges as mis LEFT JOIN sig_professorat as prof ON mis.de=prof.abrev "
//                + " WHERE instantani=2 AND para LIKE '%*%' " + cond + orderCond;
//
//
//        int k = 0;
//        try {
//            Statement st = client.getIesClient().getMysql().createStatement();
//            ResultSet rs1 = client.getIesClient().getMysql().getResultSet(SQL1, st);
//            while (rs1 != null && rs1.next()) {
//                String doc = rs1.getString("missatge");
//                AnunciBean bean = AnuncisParser.getBean(doc);
//                bean.setAbrev(rs1.getString("abrev"));
//                // bean.postdate = rs1.getDate("data");
//                bean.setNewpost(false);
//                bean.setGlobalId(k);
//                bean.setDbId(rs1.getInt("id"));
//                bean.setNou(false);
//
//                //Comprova les condicions
//                Calendar cal0 = Calendar.getInstance();
//                Calendar cal = Calendar.getInstance();
//                cal0.setTime(avui);
//                if (bean.getEventdate() != null) {
//                    cal.setTime(bean.getEventdate());
//
//                    if (mode.equalsIgnoreCase("*")
//                            || (mode.equalsIgnoreCase("A") && cal0.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH))
//                            || (mode.equalsIgnoreCase("M") && cal0.get(Calendar.MONTH) == cal.get(Calendar.MONTH))
//                            || (mode.equalsIgnoreCase("S") && cal0.get(Calendar.WEEK_OF_MONTH) == cal.get(Calendar.WEEK_OF_MONTH))) {
//                        k += 1;
//                       getListAnuncis().add(bean);
//                    }
//
//                    cal.setTime(bean.getEventdate());
//                    if (bean.getEventdate2() != null) {
//                        cal.setTime(bean.getEventdate2());
//                    }
//                    
//                    if (cal.after(cal0)) {
//                        nv += 1;
//                        
//                    }
//                }
//
//
//                bean = null;
//
//
//            }
//            if (rs1 != null) {
//                rs1.close();
//                st.close();
//            }
//        } catch (SQLException ex) {
//            Logger.getLogger(mbAnuncis.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        return nv;
//    }
}
