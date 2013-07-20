/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iesapp.web.pdaweb.data;

/**
 *
 * @author Josep
 * 
 * Model per a la taula llista d'alumnes
 * permet que sigui selectable
 * 
 */

import java.util.List;
import javax.faces.model.ListDataModel;
import org.iesapp.clients.sgd7.alumnos.BeanAlumno;
import org.primefaces.model.SelectableDataModel;
 

public class AlumnoDataModel extends ListDataModel<BeanAlumno> implements SelectableDataModel<BeanAlumno> {  

    public AlumnoDataModel() {
    }

    public AlumnoDataModel(List<BeanAlumno> data) {
        super(data);
    }
    
    @Override
    public BeanAlumno getRowData(String rowKey) {
        //In a real app, a more efficient way like a query by rowKey should be implemented to deal with huge data
        
        List<BeanAlumno> alums = (List<BeanAlumno>) getWrappedData();
        
        for(BeanAlumno alum : alums) {
            String myid = alum.getId()+"";
            if(myid.equals(rowKey)) {
                return alum;
            }
        }
        
        return null;
    }

    @Override
    public Object getRowKey(BeanAlumno alumn) {
        return alumn.getId();
    }
}
                    
