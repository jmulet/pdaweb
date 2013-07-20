/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.mbs;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import org.iesapp.clients.sgd7.mensajes.BeanProfSms;
import org.primefaces.component.picklist.PickList;
import org.primefaces.model.DualListModel;

@FacesConverter(value = "PrimeFacesPickListConverter")
public class PrimeFacesPickListConverter implements Converter {
@Override
public Object getAsObject(FacesContext arg0, UIComponent component, String value) {
    Object ret = null;
    if (component instanceof PickList) {
        Object dualList = ((PickList) component).getValue();
        DualListModel dl = (DualListModel) dualList;
        for (Object o : dl.getSource()) {
            String codigo = ((BeanProfSms) o).getCodigo();
            if (codigo.equals(value)) {
                ret = o;
                break;
            }
        }
        
    }
    return ret;
}

@Override
public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
    String str = "";
    if (arg2 instanceof BeanProfSms) {
        str = ((BeanProfSms) arg2).getCodigo();
    }
    return str;
}


}